package dev.kosh.financetracker.feature.importstatement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.statement.StatementImportResult
import dev.kosh.financetracker.core.statement.StatementParserRegistry
import dev.kosh.financetracker.core.statement.StatementTransaction
import dev.kosh.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface StatementImportUiState {
    data object Idle : StatementImportUiState
    data object Parsing : StatementImportUiState
    data class Confirming(
        val fileName: String,
        val bank: String,
        val accountSuffix: String,
        val count: Int,
        val periodLabel: String,
        private val transactions: List<StatementTransaction>,
    ) : StatementImportUiState {
        internal fun rows(): List<StatementTransaction> = transactions
    }
    data object Importing : StatementImportUiState
    data class Done(val results: List<StatementImportResult.Imported>) : StatementImportUiState
    data class Error(val message: String) : StatementImportUiState
}

@HiltViewModel
class StatementImportViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val registry = StatementParserRegistry()

    private val _uiState = MutableStateFlow<StatementImportUiState>(StatementImportUiState.Idle)
    val uiState: StateFlow<StatementImportUiState> = _uiState.asStateFlow()

    fun onFileSelected(fileName: String, bytes: ByteArray) {
        _uiState.value = StatementImportUiState.Parsing
        viewModelScope.launch {
            val rows = withContext(Dispatchers.Default) { registry.parse(fileName, bytes) }
            if (rows.isEmpty()) {
                _uiState.value = StatementImportUiState.Error(
                    "Couldn't recognize \"$fileName\" as a supported bank statement (HDFC, Kotak, or Slice).",
                )
                return@launch
            }

            // A statement covers one account; take the first group if a file
            // somehow mixed accounts (not expected from any real export).
            val accountSuffix = rows.first().accountSuffix
            val accountRows = rows.filter { it.accountSuffix == accountSuffix }
            val sorted = accountRows.sortedBy { it.timestamp }

            _uiState.value = StatementImportUiState.Confirming(
                fileName = fileName,
                bank = sorted.first().bank,
                accountSuffix = accountSuffix,
                count = sorted.size,
                periodLabel = formatPeriod(sorted),
                transactions = sorted,
            )
        }
    }

    fun confirmImport() {
        val current = _uiState.value as? StatementImportUiState.Confirming ?: return
        _uiState.value = StatementImportUiState.Importing
        viewModelScope.launch {
            val results = repository.importStatement(current.rows())
            _uiState.value = StatementImportUiState.Done(results)
        }
    }

    fun dismiss() {
        _uiState.value = StatementImportUiState.Idle
    }

    private fun formatPeriod(rows: List<StatementTransaction>): String {
        val zone = java.time.ZoneId.systemDefault()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
        val start = rows.first().timestamp.atZone(zone).format(formatter)
        val end = rows.last().timestamp.atZone(zone).format(formatter)
        return if (start == end) start else "$start – $end"
    }
}
