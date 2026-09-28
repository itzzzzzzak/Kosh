package dev.kosh.financetracker.data.repository

import dev.kosh.financetracker.core.finance.TransferDetector
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.data.database.TransactionDao
import dev.kosh.financetracker.data.database.toDomain
import dev.kosh.financetracker.data.database.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: Long): Flow<Transaction?>
    suspend fun save(transaction: Transaction): Long

    /** Returns true if actually inserted, false if a transaction from this source already existed. */
    suspend fun saveIfNew(transaction: Transaction): Boolean

    /** Re-runs transfer detection across all stored transactions and reclassifies matches. */
    suspend fun reconcileTransfers()

    suspend fun setCategory(id: Long, category: TransactionCategory)
}

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTransaction(id: Long): Flow<Transaction?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun save(transaction: Transaction): Long =
        dao.insert(transaction.toEntity())

    override suspend fun saveIfNew(transaction: Transaction): Boolean =
        dao.insertIfNew(transaction.toEntity()) != -1L

    override suspend fun reconcileTransfers() {
        val all = dao.getAll().map { it.toDomain() }
        val transferIds = TransferDetector.findTransferPairs(all)
        if (transferIds.isNotEmpty()) {
            dao.updateType(transferIds.toList(), TransactionType.TRANSFER)
        }
    }

    override suspend fun setCategory(id: Long, category: TransactionCategory) {
        dao.updateCategory(id, category)
    }
}
