package dev.kosh.financetracker.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kosh.financetracker.core.model.PaymentMethod
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSampleTransaction() {
        viewModelScope.launch {
            val now = Instant.now()
            repository.save(
                Transaction(
                    timestamp = now,
                    amount = BigDecimal("450.00"),
                    direction = TransactionDirection.DEBIT,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FOOD,
                    merchant = "Swiggy",
                    rawMerchant = "UPI-SWIGGY-BANGALORE",
                    accountId = null,
                    paymentMethod = PaymentMethod.UPI,
                    source = TransactionSource.MANUAL,
                    sourceMessageId = null,
                    confidence = 1.0,
                    notes = "Manually added sample transaction (Phase 1 proof-of-concept)",
                    createdAt = now,
                ),
            )
        }
    }
}
