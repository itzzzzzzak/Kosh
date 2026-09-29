package dev.kosh.financetracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TransactionEntity::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DB_NAME = "finance-tracker.db"
    }
}
