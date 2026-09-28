package dev.kosh.financetracker.feature.overview

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.R
import dev.kosh.financetracker.core.finance.CategorySlice
import dev.kosh.financetracker.core.finance.MonthSummary
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.ui.components.EmptyState
import dev.kosh.financetracker.ui.components.MerchantAvatar
import dev.kosh.financetracker.ui.components.SegmentedBar
import dev.kosh.financetracker.ui.components.labelFor
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@Composable
fun OverviewScreen(
    onOpenTransaction: (Long) -> Unit,
    onOpenReview: () -> Unit,
    viewModel: OverviewViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    fun hasAllSmsPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(hasAllSmsPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasPermission = hasAllSmsPermissions()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.sync()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Spacer(Modifier.height(Spacing.sm))
                OverviewHeader()
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "Kosh needs SMS access",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Kosh reads your bank SMS on-device to build your transaction history. " +
                        "Nothing leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
                        )
                    },
                ) {
                    Text("Grant SMS access")
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
            contentPadding = PaddingValues(vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item { OverviewHeader() }
            item { SpendingHero(state.monthSummary) }
            if (state.categoryBreakdown.isNotEmpty()) {
                item { CategoryBreakdownCard(state.categoryBreakdown) }
            }
            item {
                ReviewPromptRow(
                    totalCount = state.totalCount,
                    needsReviewCount = state.needsReviewCount,
                    onClick = onOpenReview,
                )
            }
            item {
                Text(
                    "Recent activity",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (state.recentActivity.isEmpty()) {
                item {
                    EmptyState(
                        title = "No activity yet",
                        description = "Grant SMS access and Kosh will pick up your bank messages automatically.",
                    )
                }
            } else {
                items(state.recentActivity, key = { it.id }) { transaction ->
                    RecentActivityRow(transaction, onClick = { onOpenTransaction(transaction.id) })
                }
            }
        }
    }
}

@Composable
private fun OverviewHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_kosh_mark),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(KoshExtendedTheme.colors.accentText),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text("Kosh", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SpendingHero(summary: MonthSummary) {
    Column {
        Text("Your month, in focus", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Spacing.md))
        Text("SPENDING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "₹${summary.expense}",
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            color = KoshExtendedTheme.colors.accentText,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Column {
                Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹${summary.income}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            }
            Column {
                Text("Spending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹${summary.expense}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(slices: List<CategorySlice>) {
    Column {
        SegmentedBar(slices = slices, colors = KoshColors.ChartPalette)
        Spacer(Modifier.height(Spacing.md))
        val rows = slices.take(4).chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            rows.forEach { rowSlices ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    rowSlices.forEachIndexed { indexInRow, slice ->
                        val overallIndex = slices.indexOf(slice)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryDot(KoshColors.ChartPalette[overallIndex % KoshColors.ChartPalette.size])
                                Spacer(Modifier.width(Spacing.xs))
                                Text(
                                    labelFor(slice.category),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "₹${slice.amount} · ${(slice.fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        if (indexInRow == 0 && rowSlices.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun ReviewPromptRow(totalCount: Int, needsReviewCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = needsReviewCount > 0, onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (needsReviewCount > 0) {
                "$totalCount transactions organized · $needsReviewCount need your review"
            } else {
                "$totalCount transactions organized"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (needsReviewCount > 0) {
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentActivityRow(transaction: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.ms),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MerchantAvatar(transaction.merchant ?: "?")
            Spacer(Modifier.width(Spacing.ms))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchant ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    transaction.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val isExpense = transaction.direction == TransactionDirection.DEBIT
            Text(
                "${if (isExpense) "-" else "+"}₹${transaction.amount}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isExpense) KoshExtendedTheme.colors.expense else KoshExtendedTheme.colors.income,
            )
        }
    }
}
