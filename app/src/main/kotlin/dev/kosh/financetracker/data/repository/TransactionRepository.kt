package dev.kosh.financetracker.data.repository

import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.data.database.TransactionDao
import dev.kosh.financetracker.data.database.toDomain
import dev.kosh.financetracker.data.database.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    suspend fun save(transaction: Transaction): Long
}

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(transaction: Transaction): Long =
        dao.insert(transaction.toEntity())
}
