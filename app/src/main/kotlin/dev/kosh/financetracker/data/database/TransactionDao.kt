package dev.kosh.financetracker.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    /** Returns -1 (and inserts nothing) when a row with the same sourceMessageId already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestampEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestampEpochMillis DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("UPDATE transactions SET type = :type WHERE id IN (:ids)")
    suspend fun updateType(ids: List<Long>, type: TransactionType)

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: TransactionCategory)

    @Query("UPDATE transactions SET category = :category WHERE id IN (:ids)")
    suspend fun updateCategories(ids: List<Long>, category: TransactionCategory)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): TransactionEntity?
}
