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

    /** Matches by [accountSuffix] alone, not bank — a statement import is the
     * authoritative record for that account+period regardless of what a prior SMS
     * parse guessed the bank was (this also naturally cleans up old rows the
     * generic UPI fallback parsed with no bank attribution). */
    @Query(
        "SELECT COUNT(*) FROM transactions WHERE accountSuffix = :accountSuffix " +
            "AND timestampEpochMillis BETWEEN :startMillis AND :endMillis",
    )
    suspend fun countForAccountInRange(accountSuffix: String, startMillis: Long, endMillis: Long): Int

    @Query(
        "DELETE FROM transactions WHERE accountSuffix = :accountSuffix " +
            "AND timestampEpochMillis BETWEEN :startMillis AND :endMillis",
    )
    suspend fun deleteForAccountInRange(accountSuffix: String, startMillis: Long, endMillis: Long)
}
