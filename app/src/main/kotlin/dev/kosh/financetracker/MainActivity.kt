package dev.kosh.financetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.kosh.financetracker.feature.inbox.SmsDebugScreen
import dev.kosh.financetracker.feature.transactions.TransactionsScreen
import dev.kosh.financetracker.ui.theme.KoshTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KoshTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    KoshApp()
                }
            }
        }
    }
}

private enum class KoshTab(val label: String) {
    TRANSACTIONS("Transactions"),
    SMS_DEBUG("SMS Debug"),
}

@Composable
private fun KoshApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            KoshTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(tab.label) },
                )
            }
        }

        when (KoshTab.entries[selectedTab]) {
            KoshTab.TRANSACTIONS -> TransactionsScreen()
            KoshTab.SMS_DEBUG -> SmsDebugScreen()
        }
    }
}
