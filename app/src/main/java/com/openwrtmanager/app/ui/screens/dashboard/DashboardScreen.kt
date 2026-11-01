package com.openwrtmanager.app.ui.screens.dashboard
import com.openwrtmanager.app.ui.common.AppStrings
import com.openwrtmanager.app.ui.common.LanguageManager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwrtmanager.app.data.model.SystemStatus
import com.openwrtmanager.app.ui.common.FullScreenError
import com.openwrtmanager.app.ui.common.FullScreenLoading
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(AppStrings.text("Router overview")) },
                actions = {
                    TextButton(onClick = {
                        LanguageManager.toggle(context)
                        (context as? android.app.Activity)?.recreate()
                    }) {
                        Text(if (LanguageManager.isArabic()) "English" else "العربية")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = AppStrings.text("Refresh"))
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLoggedOut()
                    }) {
                        Icon(Icons.Filled.Logout, contentDescription = AppStrings.text("Log out"))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading && uiState.status == null -> FullScreenLoading()
                uiState.errorMessage != null && uiState.status == null ->
                    FullScreenError(uiState.errorMessage ?: AppStrings.text("Loading failed"), onRetry = viewModel::refresh)
                uiState.status != null -> DashboardContent(
                    status = uiState.status!!,
                    onRebootClick = viewModel::requestReboot
                )
            }
        }
    }

    if (uiState.rebootRequested) {
        AlertDialog(
            onDismissRequest = viewModel::cancelReboot,
            title = { Text(AppStrings.text("Restart the router?")) },
            text = { Text(AppStrings.text("The network will be briefly interrupted and all devices will reconnect. This action cannot be undone.")) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReboot) {
                    Text(AppStrings.text("Restart"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelReboot) { Text(AppStrings.text("Cancel")) }
            }
        )
    }
}

@Composable
private fun DashboardContent(status: SystemStatus, onRebootClick: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(status.hostname, style = MaterialTheme.typography.titleLarge)
                    Text(status.model, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        status.firmwareVersion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Uptime: ${formatUptime(status.uptimeSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(AppStrings.text("Load average (1 / 5 / 15 min)"), style = MaterialTheme.typography.titleSmall)
                    Text(
                        status.loadAverage.joinToString(" / ") { String.format("%.2f", it) },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(AppStrings.text("Memory usage"), style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(
                        progress = { status.memoryUsedPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        "${status.memoryUsedPercent}% used · remaining ${status.memoryFreeKb / 1024} MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (status.rootfsTotalKb > 0) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(AppStrings.text("Storage (used to check package installation)"), style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(
                            progress = { status.rootfsUsedPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                        Text(
                            "${status.rootfsUsedPercent}% used · remaining ${status.rootfsFreeKb / 1024} MB",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(AppStrings.text("Restart router"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Dangerous operation; the network will be briefly interrupted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onRebootClick) {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            contentDescription = AppStrings.text("Restart"),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val days = TimeUnit.SECONDS.toDays(seconds)
    val hours = TimeUnit.SECONDS.toHours(seconds) % 24
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
    return buildString {
        if (days > 0) append("${days}天 ")
        append("${hours}小时 ${minutes}分钟")
    }
}
