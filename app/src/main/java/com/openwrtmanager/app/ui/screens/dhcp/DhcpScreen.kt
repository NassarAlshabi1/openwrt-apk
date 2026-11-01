package com.openwrtmanager.app.ui.screens.dhcp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwrtmanager.app.data.model.DhcpPool
import com.openwrtmanager.app.data.model.StaticLease
import com.openwrtmanager.app.ui.common.FullScreenError
import com.openwrtmanager.app.ui.common.FullScreenLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhcpScreen(viewModel: DhcpViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddLeaseDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<StaticLease?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("DHCP") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddLeaseDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add static lease")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> FullScreenLoading()
                uiState.errorMessage != null -> FullScreenError(uiState.errorMessage ?: "Loading failed", onRetry = viewModel::refresh)
                else -> {
                    if (uiState.hasPendingChanges) {
                        PendingChangesBar(
                            isApplying = uiState.isApplying,
                            onApply = viewModel::applyChanges,
                            onDiscard = viewModel::discardChanges
                        )
                    }
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                        item {
                            Text(
                                "DHCP 池 (${uiState.pools.size})",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(uiState.pools, key = { it.sectionId }) { pool ->
                            PoolItem(pool, onToggleIgnored = { viewModel.togglePoolIgnored(pool) })
                        }
                        item {
                            Text(
                                "Static leases (${uiState.staticLeases.size})",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(uiState.staticLeases, key = { it.sectionId }) { lease ->
                            LeaseItem(lease, onDelete = { pendingDelete = lease })
                        }
                    }
                }
            }
        }
    }

    if (showAddLeaseDialog) {
        AddLeaseDialog(
            onDismiss = { showAddLeaseDialog = false },
            onConfirm = { name, mac, ip ->
                viewModel.addStaticLease(name, mac, ip)
                showAddLeaseDialog = false
            }
        )
    }

    pendingDelete?.let { lease ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete static lease “${lease.name}”？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStaticLease(lease)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            text = { Text("Click “Apply changes” after deletion for it to take effect.") }
        )
    }
}

@Composable
private fun PendingChangesBar(isApplying: Boolean, onApply: () -> Unit, onDiscard: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Unsaved changes", modifier = Modifier.padding(top = 8.dp))
            Row {
                TextButton(onClick = onDiscard, enabled = !isApplying) { Text("Discard") }
                TextButton(onClick = onApply, enabled = !isApplying) { Text("Apply changes") }
            }
        }
    }
}

@Composable
private fun PoolItem(pool: DhcpPool, onToggleIgnored: () -> Unit) {
    ListItem(
        headlineContent = { Text(pool.interfaceName) },
        supportingContent = {
            Text("Start: ${pool.start ?: "-"} · Count: ${pool.limit ?: "-"} · Lease: ${pool.leaseTime ?: "Default"}")
        },
        trailingContent = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Enable DHCP", style = MaterialTheme.typography.bodySmall)
                Switch(checked = !pool.ignore, onCheckedChange = { onToggleIgnored() })
            }
        }
    )
}

@Composable
private fun LeaseItem(lease: StaticLease, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(lease.name) },
        supportingContent = { Text("${lease.macAddress} → ${lease.ipAddress}") },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLeaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, mac: String, ip: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add static lease") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Device name") }, singleLine = true)
                OutlinedTextField(value = mac, onValueChange = { mac = it }, label = { Text("MAC address") }, singleLine = true)
                OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP address") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.ifBlank { "Device" }, mac, ip) },
                enabled = mac.isNotBlank() && ip.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
