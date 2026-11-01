package com.openwrtmanager.app.ui.screens.firewall
import com.openwrtmanager.app.ui.common.AppStrings

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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwrtmanager.app.data.model.FirewallRule
import com.openwrtmanager.app.data.model.FirewallZone
import com.openwrtmanager.app.ui.common.FullScreenError
import com.openwrtmanager.app.ui.common.FullScreenLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(viewModel: FirewallViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FirewallRule?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.hasPendingChanges) {
        if (uiState.hasPendingChanges) {
            snackbarHostState.showSnackbar(AppStrings.text("Unsaved changes"), withDismissAction = false)
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(AppStrings.text("Firewall")) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = AppStrings.text("Add rule"))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> FullScreenLoading()
                uiState.errorMessage != null -> FullScreenError(uiState.errorMessage ?: AppStrings.text("Loading failed"), onRetry = viewModel::refresh)
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
                                "Zones (${uiState.zones.size})",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(uiState.zones, key = { it.sectionId }) { zone ->
                            ZoneItem(zone)
                        }
                        item {
                            Text(
                                "Rules (${uiState.rules.size})",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(uiState.rules, key = { it.sectionId }) { rule ->
                            RuleItem(
                                rule = rule,
                                onToggle = { viewModel.toggleRule(rule) },
                                onDelete = { pendingDelete = rule }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, src, dest, port, proto, target ->
                viewModel.addRule(name, src, dest, port, proto, target)
                showAddDialog = false
            }
        )
    }

    pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(AppStrings.text("Delete rule “${rule.name}”？")) },
            text = { Text(AppStrings.text("Click “Apply changes” after deletion for it to take effect.")) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRule(rule)
                    pendingDelete = null
                }) { Text(AppStrings.text("Delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(AppStrings.text("Cancel")) }
            }
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
            Text(AppStrings.text("Unsaved changes"), modifier = Modifier.padding(top = 8.dp))
            Row {
                TextButton(onClick = onDiscard, enabled = !isApplying) { Text(AppStrings.text("Discard")) }
                TextButton(onClick = onApply, enabled = !isApplying) { Text(AppStrings.text("Apply changes")) }
            }
        }
    }
}

@Composable
private fun ZoneItem(zone: FirewallZone) {
    ListItem(
        headlineContent = { Text(zone.name) },
        supportingContent = {
            Text(AppStrings.text("Input: ${zone.input} · Output: ${zone.output} · Forward: ${zone.forward}" + if (zone.masq) " · NAT" else ""))
        }
    )
}

@Composable
private fun RuleItem(rule: FirewallRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(rule.name) },
        supportingContent = {
            Text(
                buildString {
                    append(rule.src ?: "*")
                    append(" → ")
                    append(rule.dest ?: "*")
                    rule.destPort?.let { append(" :$it") }
                    rule.proto?.let { append(" ($it)") }
                    append(" · ${rule.target}")
                }
            )
        },
        trailingContent = {
            Row {
                Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = AppStrings.text("Delete"), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, src: String?, dest: String?, port: String?, proto: String?, target: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var src by remember { mutableStateOf("wan") }
    var dest by remember { mutableStateOf("lan") }
    var port by remember { mutableStateOf("") }
    var proto by remember { mutableStateOf("tcp") }
    var target by remember { mutableStateOf("ACCEPT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.text("Add firewall rule")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(AppStrings.text("Rule name")) }, singleLine = true)
                OutlinedTextField(value = src, onValueChange = { src = it }, label = { Text(AppStrings.text("Source zone (e.g. wan)")) }, singleLine = true)
                OutlinedTextField(value = dest, onValueChange = { dest = it }, label = { Text(AppStrings.text("Destination zone (e.g. lan)")) }, singleLine = true)
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text(AppStrings.text("Destination port (optional)")) }, singleLine = true)
                OutlinedTextField(value = proto, onValueChange = { proto = it }, label = { Text(AppStrings.text("Protocol (tcp/udp)")) }, singleLine = true)
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text(AppStrings.text("Action (ACCEPT/REJECT/DROP)")) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name.ifBlank { "新Rules" },
                        src.ifBlank { null },
                        dest.ifBlank { null },
                        port.ifBlank { null },
                        proto.ifBlank { null },
                        target.ifBlank { "ACCEPT" }
                    )
                },
                enabled = name.isNotBlank()
            ) { Text(AppStrings.text("Add")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.text("Cancel")) }
        }
    )
}
