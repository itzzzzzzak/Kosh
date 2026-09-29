package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.PaymentMethod
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime

class FinanceCalculatorTest {

    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 9, 28, 12, 0, 0, 0, zone).toInstant()

    private fun transaction(
        type: TransactionType,
        amount: String,
        timestamp: Instant,
        category: TransactionCategory = TransactionCategory.UNCATEGORIZED,
        bank: String? = null,
        accountSuffix: String? = null,
    ) = Transaction(
        timestamp = timestamp,
        amount = BigDecimal(amount),
        direction = if (type == TransactionType.INCOME) TransactionDirection.CREDIT else TransactionDirection.DEBIT,
        type = type,
        category = category,
        merchant = null,
        rawMerchant = null,
        accountId = null,
        accountSuffix = accountSuffix,
        bank = bank,
        paymentMethod = PaymentMethod.UPI,
        source = TransactionSource.SMS,
        sourceMessageId = null,
        rawSourceText = null,
        confidence = 1.0,
        notes = null,
        createdAt = timestamp,
    )

    @Test
    fun `sums expenses and income within the current month only`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val previousMonth = ZonedDateTime.of(2026, 8, 15, 10, 0, 0, 0, zone).toInstant()

        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "450.00", withinMonth),
            transaction(TransactionType.EXPENSE, "2000.00", withinMonth),
            transaction(TransactionType.INCOME, "49800.00", withinMonth),
            transaction(TransactionType.EXPENSE, "999.00", previousMonth),
        )

        val summary = FinanceCalculator.currentMonthSummary(transactions, zone = zone, now = now)

        assertEquals(BigDecimal("2450.00"), summary.expense)
        assertEquals(BigDecimal("49800.00"), summary.income)
        assertEquals(BigDecimal("47350.00"), summary.netCashFlow)
    }

    @Test
    fun `ignores transfer and unknown types`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.TRANSFER, "10000.00", withinMonth),
            transaction(TransactionType.UNKNOWN, "500.00", withinMonth),
        )

        val summary = FinanceCalculator.currentMonthSummary(transactions, zone = zone, now = now)

        assertEquals(BigDecimal.ZERO, summary.income)
        assertEquals(BigDecimal.ZERO, summary.expense)
    }

    @Test
    fun `returns zero summary when no transactions this month`() {
        val summary = FinanceCalculator.currentMonthSummary(emptyList(), zone = zone, now = now)

        assertEquals(BigDecimal.ZERO, summary.income)
        assertEquals(BigDecimal.ZERO, summary.expense)
        assertEquals(BigDecimal.ZERO, summary.netCashFlow)
    }

    @Test
    fun `builds a category breakdown sorted largest first with correct fractions`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "60.00", withinMonth, TransactionCategory.FOOD),
            transaction(TransactionType.EXPENSE, "20.00", withinMonth, TransactionCategory.TRAVEL),
            transaction(TransactionType.EXPENSE, "20.00", withinMonth, TransactionCategory.SHOPPING),
            transaction(TransactionType.INCOME, "1000.00", withinMonth, TransactionCategory.UNCATEGORIZED),
        )

        val breakdown = FinanceCalculator.currentMonthCategoryBreakdown(transactions, zone = zone, now = now)

        assertEquals(3, breakdown.size)
        assertEquals(TransactionCategory.FOOD, breakdown[0].category)
        assertEquals(BigDecimal("60.00"), breakdown[0].amount)
        assertEquals(0.6f, breakdown[0].fraction, 0.001f)
    }

    @Test
    fun `counts only this month's uncategorized expenses`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val previousMonth = ZonedDateTime.of(2026, 8, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "60.00", withinMonth, TransactionCategory.UNCATEGORIZED),
            transaction(TransactionType.EXPENSE, "60.00", withinMonth, TransactionCategory.FOOD),
            transaction(TransactionType.EXPENSE, "60.00", previousMonth, TransactionCategory.UNCATEGORIZED),
            transaction(TransactionType.INCOME, "60.00", withinMonth, TransactionCategory.UNCATEGORIZED),
        )

        assertEquals(1, FinanceCalculator.currentMonthUncategorizedCount(transactions, zone = zone, now = now))
    }

    @Test
    fun `groups account summaries by bank and account suffix, sorted by spend descending`() {
        val withinMonth = ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, zone).toInstant()
        val previousMonth = ZonedDateTime.of(2026, 8, 15, 10, 0, 0, 0, zone).toInstant()
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, "100.00", withinMonth, bank = "HDFC", accountSuffix = "5590"),
            transaction(TransactionType.EXPENSE, "50.00", withinMonth, bank = "HDFC", accountSuffix = "5590"),
            transaction(TransactionType.EXPENSE, "500.00", withinMonth, bank = "Kotak", accountSuffix = "9996"),
            // Excluded: no account suffix parsed, previous month, and a TRANSFER type.
            transaction(TransactionType.EXPENSE, "999.00", withinMonth, bank = "HDFC", accountSuffix = null),
            transaction(TransactionType.EXPENSE, "999.00", previousMonth, bank = "HDFC", accountSuffix = "5590"),
        )

        val summaries = FinanceCalculator.accountSummariesForMonth(transactions, YearMonth.of(2026, 9), zone)

        assertEquals(2, summaries.size)
        assertEquals("Kotak", summaries[0].bank)
        assertEquals(BigDecimal("500.00"), summaries[0].monthSpend)
        assertEquals(1, summaries[0].transactionCount)
        assertEquals("HDFC", summaries[1].bank)
        assertEquals(BigDecimal("150.00"), summaries[1].monthSpend)
        assertEquals(2, summaries[1].transactionCount)
    }
}
