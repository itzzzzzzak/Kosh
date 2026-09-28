package dev.kosh.financetracker.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.finance.CategorySlice
import dev.kosh.financetracker.core.finance.FinanceCalculator
import dev.kosh.financetracker.core.finance.MonthSummary
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.data.repository.TransactionRepository
import dev.kosh.financetracker.feature.sync.SmsSyncManager
import dev.kosh.financetracker.feature.sync.SyncState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class OverviewUiState(
    val monthSummary: MonthSummary = MonthSummary(BigDecimal.ZERO, BigDecimal.ZERO),
    val categoryBreakdown: List<CategorySlice> = emptyList(),
    val recentActivity: List<Transaction> = emptyList(),
    val totalCount: Int = 0,
    val needsReviewCount: Int = 0,
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    repository: TransactionRepository,
    private val syncManager: SmsSyncManager,
) : ViewModel() {

    val uiState: StateFlow<OverviewUiState> = repository.observeTransactions()
        .map { transactions ->
            OverviewUiState(
                monthSummary = FinanceCalculator.currentMonthSummary(transactions),
                categoryBreakdown = FinanceCalculator.currentMonthCategoryBreakdown(transactions),
                recentActivity = transactions.take(6),
                totalCount = transactions.size,
                needsReviewCount = FinanceCalculator.currentMonthUncategorizedCount(transactions),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    val syncState: StateFlow<SyncState> = syncManager.state

    /** Caller must confirm SMS permission is granted first — the ViewModel doesn't
     * check itself, since it has no Context and no way to request it. */
    fun sync() {
        viewModelScope.launch { syncManager.sync() }
    }
}
