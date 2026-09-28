package dev.kosh.financetracker.ui.components

import dev.kosh.financetracker.core.model.TransactionCategory

fun labelFor(category: TransactionCategory): String = when (category) {
    TransactionCategory.FOOD -> "Food & Dining"
    TransactionCategory.GROCERIES -> "Groceries"
    TransactionCategory.TRAVEL -> "Transport"
    TransactionCategory.FUEL -> "Fuel"
    TransactionCategory.SHOPPING -> "Shopping"
    TransactionCategory.BILLS -> "Bills & Utilities"
    TransactionCategory.SUBSCRIPTIONS -> "Subscriptions"
    TransactionCategory.RENT -> "Rent"
    TransactionCategory.FAMILY -> "Family"
    TransactionCategory.HEALTH -> "Health"
    TransactionCategory.ENTERTAINMENT -> "Entertainment"
    TransactionCategory.EDUCATION -> "Education"
    TransactionCategory.PERSONAL_CARE -> "Personal Care"
    TransactionCategory.HOME -> "Home"
    TransactionCategory.TECHNOLOGY -> "Technology"
    TransactionCategory.BUSINESS -> "Business"
    TransactionCategory.FEES -> "Fees"
    TransactionCategory.OTHER -> "Other"
    TransactionCategory.UNCATEGORIZED -> "Uncategorized"
}

/** The short list offered in the Review picker — the full enum has 19 values, too
 * many to review one at a time; these cover the overwhelming majority of spending. */
val REVIEW_PICKER_CATEGORIES = listOf(
    TransactionCategory.FOOD,
    TransactionCategory.BILLS,
    TransactionCategory.SHOPPING,
    TransactionCategory.TRAVEL,
    TransactionCategory.OTHER,
)
