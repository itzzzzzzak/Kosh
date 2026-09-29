package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MonthSummary(
    val income: BigDecimal,
    val expense: BigDecimal,
) {
    val netCashFlow: BigDecimal get() = income - expense
}

data class CategorySlice(
    val category: TransactionCategory,
    val amount: BigDecimal,
    val fraction: Float,
)

data class DaySpend(
    val date: LocalDate,
    val amount: BigDecimal,
)

data class AccountSummary(
    val bank: String?,
    val accountSuffix: String,
    val monthSpend: BigDecimal,
    val transactionCount: Int,
)

/**
 * Deliberately minimal: only sums TransactionType.INCOME and .EXPENSE. Once the
 * real type classifier exists, TRANSFER/LOAN_EMI/INVESTMENT/etc. will already be
 * excluded correctly since they're distinct types from EXPENSE/INCOME by design.
 */
object FinanceCalculator {

    fun summaryForMonth(
        transactions: List<Transaction>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MonthSummary {
        var income = BigDecimal.ZERO
        var expense = BigDecimal.ZERO

        for (transaction in transactions) {
            if (YearMonth.from(transaction.timestamp.atZone(zone)) != month) continue
            when (transaction.type) {
                TransactionType.INCOME -> income += transaction.amount
                TransactionType.EXPENSE -> expense += transaction.amount
                else -> Unit
            }
        }

        return MonthSummary(income = income, expense = expense)
    }

    fun currentMonthSummary(
        transactions: List<Transaction>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): MonthSummary = summaryForMonth(transactions, YearMonth.from(now.atZone(zone)), zone)

    /** Expense breakdown by category for the given month, sorted largest first. */
    fun categoryBreakdownForMonth(
        transactions: List<Transaction>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<CategorySlice> {
        val totalsByCategory = linkedMapOf<TransactionCategory, BigDecimal>()
        for (transaction in transactions) {
            if (transaction.type != TransactionType.EXPENSE) continue
            if (YearMonth.from(transaction.timestamp.atZone(zone)) != month) continue
            val category = transaction.category ?: TransactionCategory.UNCATEGORIZED
            totalsByCategory[category] = (totalsByCategory[category] ?: BigDecimal.ZERO) + transaction.amount
        }

        val total = totalsByCategory.values.fold(BigDecimal.ZERO, BigDecimal::add)
        if (total <= BigDecimal.ZERO) return emptyList()

        return totalsByCategory.entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                CategorySlice(
                    category = category,
                    amount = amount,
                    fraction = amount.divide(total, 4, RoundingMode.HALF_UP).toFloat(),
                )
            }
    }

    fun currentMonthCategoryBreakdown(
        transactions: List<Transaction>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): List<CategorySlice> = categoryBreakdownForMonth(transactions, YearMonth.from(now.atZone(zone)), zone)

    /** How many EXPENSE transactions in the given month still need a real category picked. */
    fun uncategorizedCountForMonth(
        transactions: List<Transaction>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int = transactions.count {
        it.type == TransactionType.EXPENSE &&
            (it.category == null || it.category == TransactionCategory.UNCATEGORIZED) &&
            YearMonth.from(it.timestamp.atZone(zone)) == month
    }

    fun currentMonthUncategorizedCount(
        transactions: List<Transaction>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): Int = uncategorizedCountForMonth(transactions, YearMonth.from(now.atZone(zone)), zone)

    /** Distinct months present in the data, newest first. */
    fun monthsWithData(
        transactions: List<Transaction>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<YearMonth> = transactions
        .map { YearMonth.from(it.timestamp.atZone(zone)) }
        .distinct()
        .sortedDescending()

    /** A real, always-browsable month selector: the last [count] calendar months up
     * to and including [referenceMonth], regardless of whether each one has data —
     * a month with nothing in it correctly shows ₹0, which is honest, not a bug. */
    fun recentMonths(
        referenceMonth: YearMonth = YearMonth.now(),
        count: Int = 12,
    ): List<YearMonth> = (0 until count).map { referenceMonth.minusMonths(it.toLong()) }

    /** Daily EXPENSE totals within the given month, in date order — only real days
     * with data are included, so a sparse month yields a sparse (honest) series. */
    fun dailySpendForMonth(
        transactions: List<Transaction>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DaySpend> {
        val totalsByDay = sortedMapOf<LocalDate, BigDecimal>()
        for (transaction in transactions) {
            if (transaction.type != TransactionType.EXPENSE) continue
            val zoned = transaction.timestamp.atZone(zone)
            if (YearMonth.from(zoned) != month) continue
            val date = zoned.toLocalDate()
            totalsByDay[date] = (totalsByDay[date] ?: BigDecimal.ZERO) + transaction.amount
        }
        return totalsByDay.map { (date, amount) -> DaySpend(date, amount) }
    }

    /** Real per-account spend for the month — grouped by the (bank, masked account
     * suffix) pair actually parsed off each SMS. Only accounts with a known suffix
     * are included; a null/unknown account can't be attributed honestly. */
    fun accountSummariesForMonth(
        transactions: List<Transaction>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<AccountSummary> {
        data class Key(val bank: String?, val accountSuffix: String)

        val totalsByAccount = linkedMapOf<Key, Pair<BigDecimal, Int>>()
        for (transaction in transactions) {
            if (transaction.type != TransactionType.EXPENSE) continue
            if (YearMonth.from(transaction.timestamp.atZone(zone)) != month) continue
            val suffix = transaction.accountSuffix ?: continue
            val key = Key(transaction.bank, suffix)
            val (sum, count) = totalsByAccount[key] ?: (BigDecimal.ZERO to 0)
            totalsByAccount[key] = (sum + transaction.amount) to (count + 1)
        }

        return totalsByAccount.entries
            .sortedByDescending { it.value.first }
            .map { (key, sumAndCount) ->
                AccountSummary(
                    bank = key.bank,
                    accountSuffix = key.accountSuffix,
                    monthSpend = sumAndCount.first,
                    transactionCount = sumAndCount.second,
                )
            }
    }

    /** Percent change in spending vs. the previous month, or null when the previous
     * month has no spending to compare against — a "0% change" would be misleading
     * there, not honest. */
    fun percentChangeVsPreviousMonth(
        transactions: List<Transaction>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Float? {
        val current = summaryForMonth(transactions, month, zone).expense
        val previous = summaryForMonth(transactions, month.minusMonths(1), zone).expense
        if (previous <= BigDecimal.ZERO) return null
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .toFloat()
    }
}
