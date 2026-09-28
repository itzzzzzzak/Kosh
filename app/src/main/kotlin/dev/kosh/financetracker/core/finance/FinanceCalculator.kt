package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth

data class MonthSummary(
    val income: BigDecimal,
    val expense: BigDecimal,
) {
    val netCashFlow: BigDecimal get() = income - expense
}

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
}
