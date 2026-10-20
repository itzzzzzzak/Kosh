package dev.kosh.financetracker.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.statement.StatementTransaction
import dev.kosh.financetracker.core.statement.toTransaction
import dev.kosh.financetracker.data.database.FinanceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class StatementImportIntegrityTest {
    private lateinit var database: FinanceDatabase
    private lateinit var repository: RoomTransactionRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FinanceDatabase::class.java,
        ).build()
        repository = RoomTransactionRepository(database.transactionDao(), database.merchantCategoryMappingDao())
    }

    @After fun tearDown() { database.close() }

    private fun row(bank: String = "HDFC", description: String = "NEW", reference: String? = null) =
        StatementTransaction(
            timestamp = Instant.parse("2026-10-01T06:00:00Z"), amount = BigDecimal("100.00"),
            direction = TransactionDirection.DEBIT, description = description, referenceNo = reference,
            balanceAfter = BigDecimal("900.00"), bank = bank, accountSuffix = "1234",
        )

    @Test fun `replacement preserves other banks and unattributed accounts with the same suffix`() = runBlocking {
        val old = row(description = "OLD").toTransaction()
        val other = row(bank = "Kotak", description = "OTHER").toTransaction()
        val unknown = row(description = "UNKNOWN").toTransaction().copy(bank = null, source = TransactionSource.SMS)
        repository.save(old)
        repository.save(other)
        repository.save(unknown)

        val result = repository.importStatement(listOf(row()))

        val stored = database.transactionDao().getAll()
        assertEquals(1, result.single().replacedCount)
        assertEquals(setOf("NEW", "OTHER", "UNKNOWN"), stored.map { it.rawMerchant }.toSet())
    }

    @Test fun `failed insert rolls back deletion and earlier inserted rows`() = runBlocking {
        repository.save(row(description = "ORIGINAL").toTransaction())
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_import BEFORE INSERT ON transactions " +
                "WHEN NEW.rawMerchant = 'FAIL' BEGIN SELECT RAISE(ABORT, 'forced import failure'); END",
        )
        val result = runCatching { repository.importStatement(listOf(row(), row(description = "FAIL"))) }

        assertTrue(result.isFailure)
        assertEquals(listOf("ORIGINAL"), database.transactionDao().getAll().map { it.rawMerchant })
    }

    @Test fun `failure in later account rolls back every imported account`() = runBlocking {
        repository.save(row(description = "ORIGINAL").toTransaction())
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_import BEFORE INSERT ON transactions " +
                "WHEN NEW.rawMerchant = 'FAIL' BEGIN SELECT RAISE(ABORT, 'forced import failure'); END",
        )
        val result = runCatching {
            repository.importStatement(listOf(row(), row(bank = "Kotak", description = "FAIL")))
        }

        assertTrue(result.isFailure)
        assertEquals(listOf("ORIGINAL"), database.transactionDao().getAll().map { it.rawMerchant })
    }

    @Test fun `rows without references and exact duplicate rows survive repeat import`() = runBlocking {
        val rows = listOf(row(description = "ONE"), row(description = "TWO"), row(description = "TWO"))
        repository.importStatement(rows)
        val first = database.transactionDao().getAll()
        assertEquals(3, first.size)
        assertEquals(3, first.map { it.sourceMessageId }.distinct().size)

        repository.importStatement(rows)
        val second = database.transactionDao().getAll()
        assertEquals(3, second.size)
        assertEquals(first.map { it.sourceMessageId }.toSet(), second.map { it.sourceMessageId }.toSet())
    }

    @Test fun `same suffix at two banks is imported as two separate accounts`() = runBlocking {
        val results = repository.importStatement(listOf(row(), row(bank = "Kotak")))
        assertEquals(setOf("HDFC", "Kotak"), results.map { it.bank }.toSet())
        assertEquals(2, database.transactionDao().getAll().size)
    }

    @Test fun `replacement preserves rows outside the imported dates`() = runBlocking {
        val outside = row(description = "OUTSIDE").copy(timestamp = Instant.parse("2026-09-01T06:00:00Z"))
        repository.save(outside.toTransaction())
        repository.importStatement(listOf(row()))
        assertEquals(setOf("NEW", "OUTSIDE"), database.transactionDao().getAll().map { it.rawMerchant }.toSet())
    }
}
