package dev.kosh.financetracker.feature.trail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.ui.components.EmptyState
import dev.kosh.financetracker.ui.components.MerchantAvatar
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TrailScreen(
    onOpenTransaction: (Long) -> Unit,
    viewModel: TrailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val zone = ZoneId.systemDefault()
    val grouped = remember(state.transactions) {
        state.transactions.groupBy { it.timestamp.atZone(zone).toLocalDate() }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Text("Kosh", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(Spacing.xs))
            Text("Trail", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "All transactions from your bank SMS",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.md))

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search descriptions, amounts or accounts") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            Spacer(Modifier.height(Spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChipRow(state.filter, viewModel::setFilter)
            }
            Spacer(Modifier.height(Spacing.sm))

            if (state.transactions.isEmpty()) {
                EmptyState(
                    title = "No transactions found",
                    description = "Try a different search or filter.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = Spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    grouped.entries.sortedByDescending { it.key }.forEach { (date, dayTransactions) ->
                        item { DateHeader(date) }
                        items(dayTransactions, key = { it.id }) { transaction ->
                            TrailRow(transaction, onClick = { onOpenTransaction(transaction.id) })
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(selected: TrailFilter, onSelect: (TrailFilter) -> Unit) {
    val options = listOf(
        TrailFilter.ALL to "All",
        TrailFilter.EXPENSES to "Expenses",
        TrailFilter.INCOME to "Income",
        TrailFilter.TRANSFERS to "Transfers",
    )
    options.forEach { (filter, label) ->
        FilterChip(
            selected = selected == filter,
            onClick = { onSelect(filter) },
            label = { Text(label) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = KoshExtendedTheme.colors.accentText.copy(alpha = 0.16f),
                selectedLabelColor = KoshExtendedTheme.colors.accentText,
            ),
        )
        Spacer(Modifier.width(Spacing.xs))
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
    val formatted = date.format(DateTimeFormatter.ofPattern("d MMM").withLocale(Locale.getDefault())).uppercase()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
    ) {
        Text(
            "$formatted $dayLabel",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrailRow(transaction: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MerchantAvatar(transaction.merchant ?: "?", size = 32.dp)
        Spacer(Modifier.width(Spacing.ms))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.merchant ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                buildString {
                    append(transaction.accountSuffix?.let { "Card •••• $it" } ?: (transaction.paymentMethod?.name ?: ""))
                    append(" · ")
                    append(transaction.source.name)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val isExpense = transaction.direction == TransactionDirection.DEBIT
            Text(
                "${if (isExpense) "-" else "+"}₹${transaction.amount}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isExpense) KoshExtendedTheme.colors.expense else KoshExtendedTheme.colors.income,
            )
            Text(
                transaction.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
