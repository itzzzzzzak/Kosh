package dev.kosh.financetracker.core.classifier

import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.model.TransactionType
import java.time.Instant

/**
 * Deliberately naive DEBIT->EXPENSE / CREDIT->INCOME mapping. This is NOT the real
 * type classifier from the plan (transfers, EMI, credit-card payments, investments
 * are all still misclassified as plain EXPENSE/INCOME here) — it exists only so
 * confirmed SMS transactions have somewhere to live before that classifier exists.
 * Category is always UNCATEGORIZED until the category engine is built.
 */
fun ParsedTransaction.toTransaction(sourceMessageId: String): Transaction {
    val type = when (direction) {
        TransactionDirection.DEBIT -> TransactionType.EXPENSE
        TransactionDirection.CREDIT -> TransactionType.INCOME
        TransactionDirection.UNKNOWN -> TransactionType.UNKNOWN
    }

    val now = Instant.now()
    return Transaction(
        timestamp = timestamp,
        amount = amount,
        direction = direction,
        type = type,
        category = TransactionCategory.UNCATEGORIZED,
        merchant = merchantRaw,
        rawMerchant = merchantRaw,
        accountId = null,
        accountSuffix = accountSuffix,
        paymentMethod = paymentMethod,
        source = TransactionSource.SMS,
        sourceMessageId = sourceMessageId,
        confidence = confidence,
        notes = null,
        createdAt = now,
    )
}
