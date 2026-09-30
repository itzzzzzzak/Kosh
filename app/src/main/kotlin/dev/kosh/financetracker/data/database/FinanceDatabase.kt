package dev.kosh.financetracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TransactionEntity::class, MerchantCategoryMappingEntity::class],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantCategoryMappingDao(): MerchantCategoryMappingDao

    companion object {
        const val DB_NAME = "finance-tracker.db"
    }
}
