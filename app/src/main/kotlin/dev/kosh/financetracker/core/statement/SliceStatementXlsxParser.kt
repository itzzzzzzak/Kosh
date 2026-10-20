package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Slice's statement export is modern .xlsx — a zip of XML files. Hand-parsed here
 * (shared-strings table + the first worksheet's cells) instead of pulling in
 * poi-ooxml, which drags in a much heavier dependency chain (xmlbeans and friends)
 * than this one format is worth.
 *
 * Real shape: metadata rows (Customer ID, Account, A/C Number, IFSC, ...), a blank
 * row, a header row (DATE, DETAILS, REF NO., TRANSACTION TYPE, DEBIT, CREDIT,
 * BALANCE), then one row per transaction to the end of the sheet (no footer).
 */
class SliceStatementXlsxParser : StatementParser {

    private val monthAbbrev = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )
    private val dateRegex = Regex("""(\d{1,2})\s+([A-Za-z]{3})\s*'?(\d{2})""")
    private val cellRefRegex = Regex("""^([A-Z]+)(\d+)$""")

    // Same limitation as the .xls case, worse: .xlsx is a zip archive, so its text
    // content is DEFLATE-compressed — a raw byte-range peek can never find plain
    // substrings in it at all. Extension is the only cheap signal; parse() bails
    // safely (returns no rows) if the expected header/account-number rows aren't found.
    override fun canParse(fileName: String, peek: String): Boolean =
        fileName.endsWith(".xlsx", ignoreCase = true)

    override fun parse(input: InputStream): List<StatementTransaction> {
        var sharedStringsBytes: ByteArray? = null
        var sheetBytes: ByteArray? = null

        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> sharedStringsBytes = zip.readBytes()
                    entry.name == "xl/worksheets/sheet1.xml" -> sheetBytes = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        val sheetData = sheetBytes ?: return emptyList()
        val sharedStrings = sharedStringsBytes?.let(::parseSharedStrings) ?: emptyList()

        val grid = parseSheetGrid(sheetData, sharedStrings)
        val accountSuffix = findAccountSuffix(grid) ?: return emptyList()

        val headerRow = grid.keys.filter { grid[it]?.get(0)?.trim().equals("DATE", ignoreCase = true) }.minOrNull()
            ?: return emptyList()

        val transactions = mutableListOf<StatementTransaction>()
        for (rowNum in grid.keys.filter { it > headerRow }.sorted()) {
            val row = grid[rowNum] ?: continue
            val date = row.getOrNull(0)?.let(::parseSliceDate) ?: continue
            val debit = row.getOrNull(4)?.toBigDecimalOrNull()
            val credit = row.getOrNull(5)?.toBigDecimalOrNull()
            val (amount, direction) = when {
                debit != null && debit > BigDecimal.ZERO -> debit to TransactionDirection.DEBIT
                credit != null && credit > BigDecimal.ZERO -> credit to TransactionDirection.CREDIT
                else -> continue
            }

            transactions += StatementTransaction(
                timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                amount = amount,
                direction = direction,
                description = row.getOrNull(1).orEmpty(),
                referenceNo = row.getOrNull(2)?.ifBlank { null },
                balanceAfter = row.getOrNull(6)?.toBigDecimalOrNull(),
                bank = "Slice",
                accountSuffix = accountSuffix,
            )
        }
        return transactions
    }

    private fun findAccountSuffix(grid: Map<Int, List<String>>): String? {
        for (row in grid.values) {
            val labelIndex = row.indexOfFirst { it.equals("A/C Number", ignoreCase = true) }
            if (labelIndex != -1 && labelIndex + 1 < row.size) {
                val digits = row[labelIndex + 1].filter { it.isDigit() }
                if (digits.length >= 4) return digits.takeLast(4)
            }
        }
        return null
    }

    private fun parseSliceDate(text: String): LocalDate? {
        val match = dateRegex.find(text) ?: return null
        val (dayStr, monStr, yyStr) = match.destructured
        val month = monthAbbrev[monStr.lowercase()] ?: return null
        val day = dayStr.toIntOrNull() ?: return null
        val year = yyStr.toIntOrNull()?.let { 2000 + it } ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlBytes.inputStream())
        val siNodes = doc.getElementsByTagName("si")
        return (0 until siNodes.length).map { i ->
            val si = siNodes.item(i) as Element
            val tNodes = si.getElementsByTagName("t")
            (0 until tNodes.length).joinToString("") { tNodes.item(it).textContent }
        }
    }

    /** Maps 1-based row number -> list of cell text values indexed by 0-based column. */
    private fun parseSheetGrid(xmlBytes: ByteArray, sharedStrings: List<String>): Map<Int, List<String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlBytes.inputStream())
        val rowNodes = doc.getElementsByTagName("row")
        val grid = linkedMapOf<Int, MutableList<String>>()

        for (r in 0 until rowNodes.length) {
            val rowEl = rowNodes.item(r) as Element
            val rowNum = rowEl.getAttribute("r").toIntOrNull() ?: continue
            val cells = mutableListOf<String>()
            val cellNodes = rowEl.getElementsByTagName("c")
            for (c in 0 until cellNodes.length) {
                val cellEl = cellNodes.item(c) as Element
                val ref = cellEl.getAttribute("r")
                val colIndex = columnIndex(ref) ?: continue
                val type = cellEl.getAttribute("t")
                val vNodes = cellEl.getElementsByTagName("v")
                val rawValue = if (vNodes.length > 0) vNodes.item(0).textContent else ""
                val value = if (type == "s") {
                    rawValue.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                } else {
                    rawValue
                }
                while (cells.size <= colIndex) cells.add("")
                cells[colIndex] = value
            }
            grid[rowNum] = cells
        }
        return grid
    }

    private fun columnIndex(cellRef: String): Int? {
        val letters = cellRefRegex.find(cellRef)?.groupValues?.get(1) ?: return null
        var index = 0
        for (ch in letters) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return index - 1
    }
}
