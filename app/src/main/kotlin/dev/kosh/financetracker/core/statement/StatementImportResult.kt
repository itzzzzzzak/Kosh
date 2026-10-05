package dev.kosh.financetracker.core.statement

import java.time.LocalDate

sealed interface StatementImportResult {
    /** The file didn't match any known bank statement format. */
    data object Unrecognized : StatementImportResult

    data class Imported(
        val bank: String,
        val accountSuffix: String,
        val importedCount: Int,
        val replacedCount: Int,
        val periodStart: LocalDate,
        val periodEnd: LocalDate,
    ) : StatementImportResult
}
