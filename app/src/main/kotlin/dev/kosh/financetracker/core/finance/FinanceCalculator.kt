package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth

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

/**
 * Deliberately minimal: only sums TransactionType.INCOME and .EXPENSE. Once the
 * real type classifier exists, TRANSFER/LOAN_EMI/INVESTMENT/etc. will already be
 * excluded correctly since they're distinct types from EXPENSE/INCOME by design.
 */
object FinanceCalculator {

    fun currentMonthSummary(
        transactions: List<Transaction>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): MonthSummary {
        val currentMonth = YearMonth.from(now.atZone(zone))

        var income = BigDecimal.ZERO
        var expense = BigDecimal.ZERO

        for (transaction in transactions) {
            if (YearMonth.from(transaction.timestamp.atZone(zone)) != currentMonth) continue
            when (transaction.type) {
                TransactionType.INCOME -> income += transaction.amount
                TransactionType.EXPENSE -> expense += transaction.amount
                else -> Unit
            }
        }

        return MonthSummary(income = income, expense = expense)
    }

    /** Expense breakdown by category for the current month, sorted largest first. */
    fun currentMonthCategoryBreakdown(
        transactions: List<Transaction>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): List<CategorySlice> {
        val currentMonth = YearMonth.from(now.atZone(zone))

        val totalsByCategory = linkedMapOf<TransactionCategory, BigDecimal>()
        for (transaction in transactions) {
            if (transaction.type != TransactionType.EXPENSE) continue
            if (YearMonth.from(transaction.timestamp.atZone(zone)) != currentMonth) continue
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

    /** How many EXPENSE transactions this month still need a real category picked. */
    fun currentMonthUncategorizedCount(
        transactions: List<Transaction>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): Int {
        val currentMonth = YearMonth.from(now.atZone(zone))
        return transactions.count {
            it.type == TransactionType.EXPENSE &&
                (it.category == null || it.category == TransactionCategory.UNCATEGORIZED) &&
                YearMonth.from(it.timestamp.atZone(zone)) == currentMonth
        }
    }
}
