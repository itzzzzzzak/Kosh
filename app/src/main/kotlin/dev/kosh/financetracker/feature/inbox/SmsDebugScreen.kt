package dev.kosh.financetracker.feature.inbox

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.data.sms.SmsMessageEntry
import dev.kosh.financetracker.ui.components.EmptyState
import dev.kosh.financetracker.ui.theme.KoshColors
import dev.kosh.financetracker.ui.theme.Spacing

@Composable
fun SmsDebugScreen(viewModel: SmsDebugViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showUnconfirmed by remember { mutableStateOf(false) }

    fun hasAllSmsPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(hasAllSmsPermissions()) }

    // Both are separate "dangerous" runtime permissions — declaring RECEIVE_SMS in the
    // manifest alone does NOT grant it; live capture silently never fires without this.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasPermission = hasAllSmsPermissions()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadMessages()
    }

    Scaffold(containerColor = KoshColors.Background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.ms),
        ) {
            when {
                !hasPermission -> {
                    Text(
                        "Kosh needs SMS access to detect financial messages on this device.",
                        color = KoshColors.Foreground,
                    )
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
                            )
                        },
                    ) {
                        Text("Grant SMS permission")
                    }
                }

                state.isLoading -> {
                    CircularProgressIndicator(color = KoshColors.Primary)
                    Text("Scanning entire inbox…", color = KoshColors.MutedForeground)
                }

                else -> {
                    Text(
                        "Scanned ${state.totalScanned} messages · ${state.transactions.size} confirmed " +
                            "transactions · ${state.unconfirmed.size} unclear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KoshColors.Foreground,
                    )
                    Text(
                        "${state.newlyImported} newly saved to ledger this scan",
                        style = MaterialTheme.typography.labelMedium,
                        color = KoshColors.MutedForeground,
                    )
                    Button(onClick = { viewModel.loadMessages() }) {
                        Text("Rescan")
                    }

                    if (state.transactions.isEmpty()) {
                        EmptyState(
                            title = "No confirmed inflow/outflow found yet",
                            description = "Confirmed transactions from your SMS inbox will show up here.",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(state.transactions, key = { it.entry.id }) { row ->
                                SmsDebugRow(row.entry, row.parsed)
                            }
                        }
                    }

                    if (state.unconfirmed.isNotEmpty()) {
                        TextButton(onClick = { showUnconfirmed = !showUnconfirmed }) {
                            Text(
                                if (showUnconfirmed) {
                                    "Hide ${state.unconfirmed.size} unclear messages"
                                } else {
                                    "Show ${state.unconfirmed.size} unclear financial-looking messages"
                                },
                            )
                        }

                        if (showUnconfirmed) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                items(state.unconfirmed, key = { it.entry.id }) { row ->
                                    SmsDebugRow(row.entry, row.parsed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsDebugRow(entry: SmsMessageEntry, parsed: ParsedTransaction?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KoshColors.SurfaceVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Spacing.ms)) {
            Text(
                entry.sender.ifBlank { "Unknown sender" },
                style = MaterialTheme.typography.labelMedium,
                color = KoshColors.MutedForeground,
            )
            Text(
                entry.body,
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium,
                color = KoshColors.Foreground,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xs),
            )

            if (parsed != null) {
                Text(
                    "Amount ₹${parsed.amount} · ${parsed.direction} · " +
                        "${parsed.bank ?: "Unrecognized bank"} · ${parsed.paymentMethod ?: "?"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = KoshColors.Foreground,
                )
                Text(
                    "Account ${parsed.accountSuffix ?: "?"} · Merchant ${parsed.merchantRaw ?: "?"} · " +
                        "Confidence ${"%.2f".format(parsed.confidence)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = KoshColors.MutedForeground,
                )
            } else {
                Text(
                    "Not parsed by any parser yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = KoshColors.MutedForeground,
                )
            }
        }
    }
}
