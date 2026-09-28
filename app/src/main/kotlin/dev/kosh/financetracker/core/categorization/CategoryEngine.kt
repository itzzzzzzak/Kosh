package dev.kosh.financetracker.core.categorization

import dev.kosh.financetracker.core.model.TransactionCategory

/**
 * Keyword-rule tier only, per the plan's decision order (user rule > merchant rule >
 * historical mapping > keyword rule > generic rule > UNKNOWN). The earlier tiers —
 * per-user overrides and learned merchant mappings — need a corrections UI and a
 * merchant-mapping store, neither of which exist yet; this is the fallback until then.
 */
object CategoryEngine {

    private val RULES: List<Pair<TransactionCategory, List<String>>> = listOf(
        TransactionCategory.GROCERIES to listOf(
            "blinkit", "zepto", "bigbasket", "grofers", "dmart", "grocery", "instamart", "supermarket", "dairy",
        ),
        TransactionCategory.FOOD to listOf(
            "swiggy", "zomato", "restaurant", "cafe", "kitchen", "biryani", "pizza", "domino", "mcdonald",
            "kfc", "burger", "dhaba", "idli", "dosa", "food", "eatery", "bakery", "chicken",
        ),
        TransactionCategory.TRAVEL to listOf(
            "uber", "ola", "rapido", "irctc", "redbus", "indigo", "spicejet", "railway", "metro", "cab",
        ),
        TransactionCategory.FUEL to listOf(
            "petrol", "diesel", "fuel", "indian oil", "bharat petroleum", "hp petrol",
        ),
        TransactionCategory.SUBSCRIPTIONS to listOf(
            "netflix", "spotify", "prime video", "hotstar", "subscription", "youtube premium",
        ),
        TransactionCategory.BILLS to listOf(
            "electricity", "water bill", "recharge", "postpaid", "prepaid", "airtel", "jio", "vodafone",
            "vi ", "bsnl", "broadband", "gas bill",
        ),
        TransactionCategory.RENT to listOf("rent"),
        TransactionCategory.HEALTH to listOf(
            "pharmacy", "hospital", "clinic", "medical", "apollo", "medplus", "diagnostic",
        ),
        TransactionCategory.ENTERTAINMENT to listOf(
            "bookmyshow", "pvr", "inox", "cinema",
        ),
        TransactionCategory.EDUCATION to listOf(
            "school", "college", "university", "udemy", "byju",
        ),
        TransactionCategory.PERSONAL_CARE to listOf(
            "salon", "spa", "hair dresser", "beauty",
        ),
        TransactionCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "retail", "mall", "store",
        ),
    )

    fun categorize(merchant: String?): TransactionCategory {
        if (merchant.isNullOrBlank()) return TransactionCategory.UNCATEGORIZED
        val lower = merchant.lowercase()
        for ((category, keywords) in RULES) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return TransactionCategory.UNCATEGORIZED
    }
}
