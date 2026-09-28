package dev.kosh.financetracker.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.core.finance.MonthSummary
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.ui.components.EmptyState
import dev.kosh.financetracker.ui.components.MerchantAvatar
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.Spacing

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val monthSummary by viewModel.monthSummary.collectAsState()

    Scaffold(containerColor = KoshColors.Background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Text("Kosh", style = MaterialTheme.typography.titleLarge, color = KoshColors.Foreground)

            MonthSummaryCard(monthSummary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, color = KoshColors.Foreground)
                TextButton(onClick = { viewModel.addSampleTransaction() }) {
                    Text("+ Add sample")
                }
            }

            if (transactions.isEmpty()) {
                EmptyState(
                    title = "No transactions yet",
                    description = "Import from the SMS Debug tab, or add a sample transaction to see it here.",
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionRow(transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(summary: MonthSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(KoshColors.GradientStart, KoshColors.GradientEnd)))
            .padding(Spacing.lg),
    ) {
        Text("This Month", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        Text(
            "₹${summary.netCashFlow}",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            Column {
                Text("Income", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                Text("₹${summary.income}", color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Column {
                Text("Expense", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                Text("₹${summary.expense}", color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MerchantAvatar(transaction.merchant ?: "?")
        Spacer(Modifier.width(Spacing.ms))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.merchant ?: "Unknown", style = MaterialTheme.typography.bodyLarge, color = KoshColors.Foreground)
            Text(
                "${transaction.type} · ${transaction.paymentMethod ?: "?"}",
                style = MaterialTheme.typography.labelMedium,
                color = KoshColors.MutedForeground,
            )
        }
        val isExpense = transaction.direction == TransactionDirection.DEBIT
        Text(
            "${if (isExpense) "-" else "+"}₹${transaction.amount}",
            color = if (isExpense) KoshColors.Destructive else KoshColors.Success,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
