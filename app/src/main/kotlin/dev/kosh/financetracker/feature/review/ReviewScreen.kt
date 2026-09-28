package dev.kosh.financetracker.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.ui.components.EmptyState
import dev.kosh.financetracker.ui.components.MerchantAvatar
import dev.kosh.financetracker.ui.components.REVIEW_PICKER_CATEGORIES
import dev.kosh.financetracker.ui.components.labelFor
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReviewScreen(viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

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
            Text("Review", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Help us categorize a few transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))

            val current = state.current
            if (current == null) {
                EmptyState(
                    title = "All caught up",
                    description = "Nothing needs your review right now.",
                )
            } else {
                ReviewCard(
                    transaction = current,
                    position = state.position,
                    total = state.queue.size,
                    selectedCategory = state.selectedCategory,
                    onPrevious = viewModel::goToPrevious,
                    onNext = viewModel::goToNext,
                    onSelectCategory = viewModel::selectCategory,
                    onConfirm = viewModel::confirmSelection,
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(
    transaction: Transaction,
    position: Int,
    total: Int,
    selectedCategory: TransactionCategory?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectCategory: (TransactionCategory) -> Unit,
    onConfirm: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${position + 1} of $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                IconButton(onClick = onPrevious, enabled = position > 0) {
                    Text(
                        "‹",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (position > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNext, enabled = position < total - 1) {
                    Text(
                        "›",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (position < total - 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            "₹${transaction.amount}",
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Text(
            transaction.timestamp.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM yyyy · h:mm a")),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            "We couldn't confidently categorize this transaction. It looks like a " +
                "${transaction.paymentMethod?.name?.lowercase() ?: "card"} payment, but the merchant is unclear.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.md),
        )

        if (!transaction.rawSourceText.isNullOrBlank()) {
            Text(
                "Source message (from bank SMS)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs, bottom = Spacing.md),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    transaction.rawSourceText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(Spacing.ms),
                )
            }
        }

        Text(
            "What type of transaction is this?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            REVIEW_PICKER_CATEGORIES.forEach { category ->
                CategoryOptionRow(
                    category = category,
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Button(
            onClick = onConfirm,
            enabled = selectedCategory != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = KoshExtendedTheme.colors.accentText,
                contentColor = MaterialTheme.colorScheme.background,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Confirm type", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun CategoryOptionRow(
    category: TransactionCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) KoshExtendedTheme.colors.accentText else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.ms, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MerchantAvatar(labelFor(category), size = 28.dp)
        Spacer(Modifier.width(Spacing.ms))
        Text(
            labelFor(category),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = KoshExtendedTheme.colors.accentText),
        )
    }
}
