package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionType
import java.time.Duration

/**
 * Finds pairs of transactions that are really the same money moving between two of
 * the user's own accounts (e.g. HDFC -> Kotak) rather than two separate real events.
 *
 * Heuristic, not a ledger reconciliation: same amount, opposite direction, close in
 * time, and — when both sides have a known account suffix — from different accounts.
 * No "owned accounts" registry exists yet (Phase 19), so this can't be fully certain;
 * it deliberately only matches EXPENSE/INCOME pairs so it never reclassifies a
 * transaction that's already something more specific (EMI, investment, etc).
 */
object TransferDetector {

    private val TIME_WINDOW: Duration = Duration.ofMinutes(15)

    /** Returns the ids of transactions that form a transfer pair. */
    fun findTransferPairs(transactions: List<Transaction>): Set<Long> {
        // UNKNOWN is included deliberately: since IncomeClassifier narrowed CREDIT->INCOME
        // to salary-only, a self-transfer's incoming leg is UNKNOWN (not INCOME) until this
        // detector re-labels it TRANSFER. Excluding UNKNOWN here silently broke matching for
        // every non-salary credit — the exact case this detector exists to catch.
        val candidates = transactions.filter {
            it.type == TransactionType.EXPENSE ||
                it.type == TransactionType.INCOME ||
                it.type == TransactionType.UNKNOWN
        }
        val matched = mutableSetOf<Long>()

        for (i in candidates.indices) {
            val a = candidates[i]
            if (a.id in matched) continue

            for (j in i + 1 until candidates.size) {
                val b = candidates[j]
                if (b.id in matched) continue
                // BigDecimal.equals() treats "5210" and "5210.00" as different (scale
                // differs); compareTo() is the numeric comparison we actually want here.
                if (a.amount.compareTo(b.amount) != 0) continue
                if (a.direction == b.direction) continue
                if (a.accountSuffix != null && a.accountSuffix == b.accountSuffix) continue
                if (Duration.between(a.timestamp, b.timestamp).abs() > TIME_WINDOW) continue

                matched += a.id
                matched += b.id
                break
            }
        }

        return matched
    }
}
