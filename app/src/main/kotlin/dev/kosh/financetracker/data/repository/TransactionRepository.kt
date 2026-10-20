package dev.kosh.financetracker.data.repository

import androidx.room.withTransaction
import dev.kosh.financetracker.core.categorization.CategoryEngine
import dev.kosh.financetracker.core.classifier.IncomeClassifier
import dev.kosh.financetracker.core.finance.TransferDetector
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.core.statement.StatementImportResult
import dev.kosh.financetracker.core.statement.StatementTransaction
import dev.kosh.financetracker.core.statement.toTransaction
import dev.kosh.financetracker.data.database.MerchantCategoryMappingDao
import dev.kosh.financetracker.data.database.FinanceDatabase
import dev.kosh.financetracker.data.database.MerchantCategoryMappingEntity
import dev.kosh.financetracker.data.database.TransactionDao
import dev.kosh.financetracker.data.database.toDomain
import dev.kosh.financetracker.data.database.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: Long): Flow<Transaction?>
    suspend fun save(transaction: Transaction): Long

    /** Returns true if actually inserted, false if a transaction from this source already existed. */
    suspend fun saveIfNew(transaction: Transaction): Boolean

    /** Re-runs transfer detection across all stored transactions and reclassifies matches. */
    suspend fun reconcileTransfers()

    /** Re-runs the salary/income heuristic across all stored CREDIT transactions —
     * fixes rows already imported under an older, looser classification rule. */
    suspend fun reconcileIncomeClassification()

    /** Re-runs CategoryEngine against every still-UNCATEGORIZED row — picks up new
     * keyword-rule coverage retroactively without touching rows a user already
     * confirmed via Review/Detail (those are no longer UNCATEGORIZED). */
    suspend fun reconcileCategories()

    /** Sets [id]'s category and, when it has a merchant, remembers the choice so
     * every other (and future) transaction from that same merchant is categorized
     * the same way without needing review again. */
    suspend fun setCategory(id: Long, category: TransactionCategory)

    /** Imports parsed statement rows as the ground truth for the account+period
     * they cover — every existing transaction for that bank and account (any
     * source) within the statement's date range is deleted and replaced. One
     * result per bank/account pair. Replacement and reconciliation commit together
     * or roll back together on failure or cancellation. Unknown-bank rows survive. */
    suspend fun importStatement(transactions: List<StatementTransaction>): List<StatementImportResult.Imported>
}

private fun normalizeMerchant(merchant: String?): String? =
    merchant?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val database: FinanceDatabase,
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

    override suspend fun reconcileIncomeClassification() {
        val all = dao.getAll().map { it.toDomain() }
        val toIncome = mutableListOf<Long>()
        val toUnknown = mutableListOf<Long>()

        for (transaction in all) {
            if (transaction.direction != TransactionDirection.CREDIT) continue
            if (transaction.type != TransactionType.INCOME && transaction.type != TransactionType.UNKNOWN) continue

            val shouldBeIncome = IncomeClassifier.isLikelySalary(transaction.merchant, transaction.rawSourceText)
            when {
                shouldBeIncome && transaction.type != TransactionType.INCOME -> toIncome += transaction.id
                !shouldBeIncome && transaction.type == TransactionType.INCOME -> toUnknown += transaction.id
            }
        }

        if (toIncome.isNotEmpty()) dao.updateType(toIncome, TransactionType.INCOME)
        if (toUnknown.isNotEmpty()) dao.updateType(toUnknown, TransactionType.UNKNOWN)
    }

    override suspend fun reconcileCategories() {
        val all = dao.getAll().map { it.toDomain() }
        val learnedMappings = merchantCategoryMappingDao.getAll()
            .associate { it.merchant to it.category }
        val idsByNewCategory = mutableMapOf<TransactionCategory, MutableList<Long>>()

        for (transaction in all) {
            if (transaction.category != TransactionCategory.UNCATEGORIZED && transaction.category != null) continue
            // Historical mapping (a category the user already picked for this exact
            // merchant) takes priority over the generic keyword-rule tier.
            val recategorized = normalizeMerchant(transaction.merchant)?.let { learnedMappings[it] }
                ?: CategoryEngine.categorize(transaction.merchant)
            if (recategorized != TransactionCategory.UNCATEGORIZED) {
                idsByNewCategory.getOrPut(recategorized) { mutableListOf() } += transaction.id
            }
        }

        idsByNewCategory.forEach { (category, ids) -> dao.updateCategories(ids, category) }
    }

    override suspend fun setCategory(id: Long, category: TransactionCategory) {
        dao.updateCategory(id, category)

        val merchant = normalizeMerchant(dao.getByIdOnce(id)?.merchant) ?: return
        merchantCategoryMappingDao.upsert(
            MerchantCategoryMappingEntity(
                merchant = merchant,
                category = category,
                updatedAtEpochMillis = Instant.now().toEpochMilli(),
            ),
        )

        // Immediately propagate to every other still-unresolved transaction from the
        // same merchant, not just future imports — the whole point is "never ask
        // about this merchant again."
        val sameMerchantIds = dao.getAll()
            .filter {
                it.id != id &&
                    normalizeMerchant(it.merchant) == merchant &&
                    (it.category == TransactionCategory.UNCATEGORIZED || it.category == null)
            }
            .map { it.id }
        if (sameMerchantIds.isNotEmpty()) {
            dao.updateCategories(sameMerchantIds, category)
        }
    }

    override suspend fun importStatement(transactions: List<StatementTransaction>): List<StatementImportResult.Imported> {
        if (transactions.isEmpty()) return emptyList()
        require(transactions.all { it.bank.isNotBlank() && it.accountSuffix.isNotBlank() }) {
            "Statement rows must identify a bank and account"
        }
        val zone = ZoneId.systemDefault()
        return database.withTransaction {
            val results = mutableListOf<StatementImportResult.Imported>()

            val accounts = transactions.groupBy { it.bank.trim().lowercase(Locale.ROOT) to it.accountSuffix.trim() }
            for ((account, rows) in accounts) {
                val (bank, accountSuffix) = account
                val periodStart = rows.minOf { it.timestamp }.atZone(zone).toLocalDate()
                val periodEnd = rows.maxOf { it.timestamp }.atZone(zone).toLocalDate()
                val startMillis = periodStart.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = periodEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                val replacedCount = dao.countForAccountInRange(bank, accountSuffix, startMillis, endMillis)
                // Exact duplicate rows can be legitimate. Keep their multiplicity,
                // while deriving the same set of identifiers on a repeated import.
                val occurrences = mutableMapOf<String, Int>()
                val entities = rows.map { row ->
                    val transaction = row.toTransaction()
                    val key = requireNotNull(transaction.sourceMessageId)
                    val occurrence = occurrences.getOrDefault(key, 0)
                    occurrences[key] = occurrence + 1
                    transaction.copy(sourceMessageId = "$key:$occurrence").toEntity()
                }
                dao.deleteForAccountInRange(bank, accountSuffix, startMillis, endMillis)
                dao.insertStatementRows(entities)

                results += StatementImportResult.Imported(
                    bank = rows.first().bank.trim(),
                    accountSuffix = accountSuffix,
                    importedCount = rows.size,
                    replacedCount = replacedCount,
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                )
            }

            reconcileTransfers()
            reconcileCategories()
            reconcileIncomeClassification()
            results
        }
    }
}
