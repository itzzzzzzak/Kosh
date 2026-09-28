package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.RawFinancialEvent
import javax.inject.Inject

/**
 * Tries bank-specific parsers first (in order), falls back to the generic UPI
 * parser last. First parser that both claims and successfully parses wins.
 */
class TransactionParserRegistry @Inject constructor() {

    private val parsers: List<TransactionParser> = listOf(
        HdfcParser(),
        KotakParser(),
        GenericUpiParser(),
    )

    fun parse(event: RawFinancialEvent): ParsedTransaction? =
        parsers.firstOrNull { it.canParse(event) }?.parse(event)
}
