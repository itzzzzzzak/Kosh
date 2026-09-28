package dev.kosh.financetracker.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.classifier.FinancialMessageDetector
import dev.kosh.financetracker.core.classifier.toTransaction
import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.parser.TransactionParserRegistry
import dev.kosh.financetracker.data.repository.TransactionRepository
import dev.kosh.financetracker.data.sms.SmsMessageEntry
import dev.kosh.financetracker.data.sms.SmsReader
import dev.kosh.financetracker.data.sms.smsSourceMessageId
import dev.kosh.financetracker.data.sms.toRawFinancialEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SmsDebugRowState(
    val entry: SmsMessageEntry,
    val parsed: ParsedTransaction?,
)

data class SmsDebugState(
    val isLoading: Boolean = false,
    val totalScanned: Int = 0,
    val transactions: List<SmsDebugRowState> = emptyList(),
    val unconfirmed: List<SmsDebugRowState> = emptyList(),
    val newlyImported: Int = 0,
)

@HiltViewModel
class SmsDebugViewModel @Inject constructor(
    private val smsReader: SmsReader,
    private val parserRegistry: TransactionParserRegistry,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SmsDebugState())
    val state: StateFlow<SmsDebugState> = _state.asStateFlow()

    fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val result = withContext(Dispatchers.IO) {
                val all = smsReader.readInboxMessages()
                val financial = all.filter { FinancialMessageDetector.isFinancialMessage(it.body) }
                val rows = financial.map { entry ->
                    SmsDebugRowState(entry = entry, parsed = parserRegistry.parse(entry.toRawFinancialEvent()))
                }

                val isConfirmedTransaction: (SmsDebugRowState) -> Boolean = { row ->
                    row.parsed != null && row.parsed.direction != TransactionDirection.UNKNOWN
                }
                val transactions = rows.filter(isConfirmedTransaction)
                val unconfirmed = rows.filterNot(isConfirmedTransaction)

                var imported = 0
                for (row in transactions) {
                    val sourceMessageId = smsSourceMessageId(
                        row.entry.sender,
                        row.entry.timestampEpochMillis,
                        row.entry.body,
                    )
                    val transaction = row.parsed!!.toTransaction(sourceMessageId = sourceMessageId)
                    if (transactionRepository.saveIfNew(transaction)) imported++
                }

                // Always reconcile, not just when something new was imported — fixes
                // already-stored rows too (e.g. after a TransferDetector bug fix).
                transactionRepository.reconcileTransfers()

                Triple(all.size, transactions, unconfirmed) to imported
            }

            val (scanTriple, imported) = result
            val (totalScanned, transactions, unconfirmed) = scanTriple
            _state.value = SmsDebugState(
                isLoading = false,
                totalScanned = totalScanned,
                transactions = transactions,
                unconfirmed = unconfirmed,
                newlyImported = imported,
            )
        }
    }
}
