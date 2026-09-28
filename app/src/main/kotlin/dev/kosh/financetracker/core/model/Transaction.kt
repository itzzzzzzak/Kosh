package dev.kosh.financetracker.core.model

import java.math.BigDecimal
import java.time.Instant

enum class TransactionDirection {
    CREDIT,
    DEBIT,
    UNKNOWN,
}

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    CREDIT_CARD_PURCHASE,
    CREDIT_CARD_PAYMENT,
    LOAN_EMI,
    LOAN_DISBURSEMENT,
    INVESTMENT,
    SAVINGS,
    REFUND,
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    FEE,
    INTEREST_INCOME,
    UNKNOWN,
}

enum class TransactionCategory {
    FOOD,
    GROCERIES,
    TRAVEL,
    FUEL,
    SHOPPING,
    BILLS,
    SUBSCRIPTIONS,
    RENT,
    FAMILY,
    HEALTH,
    ENTERTAINMENT,
    EDUCATION,
    PERSONAL_CARE,
    HOME,
    TECHNOLOGY,
    BUSINESS,
    FEES,
    OTHER,
    UNCATEGORIZED,
}

enum class PaymentMethod {
    UPI,
    CARD,
    NEFT,
    IMPS,
    RTGS,
    CASH,
    NETBANKING,
    UNKNOWN,
}

enum class TransactionSource {
    SMS,
    NOTIFICATION,
    MANUAL,
}

data class Transaction(
    val id: Long = 0,
    val timestamp: Instant,
    val amount: BigDecimal,
    val direction: TransactionDirection,
    val type: TransactionType,
    val category: TransactionCategory?,
    val merchant: String?,
    val rawMerchant: String?,
    val accountId: Long?,
    val paymentMethod: PaymentMethod?,
    val source: TransactionSource,
    val sourceMessageId: String?,
    val confidence: Double,
    val notes: String?,
    val createdAt: Instant,
)
