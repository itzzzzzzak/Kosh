package dev.kosh.financetracker.core.categorization

import dev.kosh.financetracker.core.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryEngineTest {

    @Test
    fun `categorizes known food merchants`() {
        assertEquals(TransactionCategory.FOOD, CategoryEngine.categorize("SWIGGY"))
        assertEquals(TransactionCategory.FOOD, CategoryEngine.categorize("Nothing But Chicken Dadar"))
    }

    @Test
    fun `categorizes groceries before food when both could match`() {
        assertEquals(TransactionCategory.GROCERIES, CategoryEngine.categorize("BLINKIT"))
    }

    @Test
    fun `categorizes rides as travel`() {
        assertEquals(TransactionCategory.TRAVEL, CategoryEngine.categorize("Rapido QR Pay"))
    }

    @Test
    fun `categorizes retail as shopping`() {
        assertEquals(TransactionCategory.SHOPPING, CategoryEngine.categorize("RELIANCE RETAIL LIMITIED"))
    }

    @Test
    fun `falls back to uncategorized for a plain person name`() {
        assertEquals(TransactionCategory.UNCATEGORIZED, CategoryEngine.categorize("AMOL KALEL"))
    }

    @Test
    fun `falls back to uncategorized for null or blank merchant`() {
        assertEquals(TransactionCategory.UNCATEGORIZED, CategoryEngine.categorize(null))
        assertEquals(TransactionCategory.UNCATEGORIZED, CategoryEngine.categorize(""))
    }
}
