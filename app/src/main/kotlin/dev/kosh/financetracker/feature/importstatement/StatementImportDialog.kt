package dev.kosh.financetracker.feature.importstatement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.KoshExtendedTheme
import dev.kosh.financetracker.ui.theme.Spacing

@Composable
fun StatementImportDialog(
    state: StatementImportUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is StatementImportUiState.Idle -> Unit

        is StatementImportUiState.Parsing, is StatementImportUiState.Importing -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = KoshExtendedTheme.colors.accentText)
                    Spacer(Modifier.height(0.dp))
                    Text(
                        if (state is StatementImportUiState.Parsing) "  Reading statement…" else "  Importing…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            containerColor = KoshColors.Graphite,
        )

        is StatementImportUiState.Confirming -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Import ${state.bank} statement?") },
            text = {
                Column {
                    Text(
                        "${state.bank} ••${state.accountSuffix} · ${state.periodLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KoshColors.PrimaryText,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "${state.count} transactions found. This will replace any existing data for this " +
                            "account over that period — the statement becomes the source of truth for it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KoshColors.SecondaryText,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Import", color = KoshExtendedTheme.colors.accentText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel", color = KoshColors.SecondaryText) }
            },
            containerColor = KoshColors.Graphite,
        )

        is StatementImportUiState.Done -> {
            val result = state.results.firstOrNull()
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Statement imported") },
                text = {
                    Text(
                        if (result != null) {
                            "${result.bank} ••${result.accountSuffix}: imported ${result.importedCount} transactions" +
                                if (result.replacedCount > 0) ", replacing ${result.replacedCount} existing ones for that period." else "."
                        } else {
                            "Done."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = KoshColors.PrimaryText,
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("OK", color = KoshExtendedTheme.colors.accentText) }
                },
                containerColor = KoshColors.Graphite,
            )
        }

        is StatementImportUiState.Error -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Couldn't import") },
            text = { Text(state.message, style = MaterialTheme.typography.bodyMedium, color = KoshColors.SecondaryText) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK", color = KoshExtendedTheme.colors.accentText) }
            },
            containerColor = KoshColors.Graphite,
        )
    }
}
