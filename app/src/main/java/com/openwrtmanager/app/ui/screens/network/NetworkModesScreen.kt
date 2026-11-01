package com.openwrtmanager.app.ui.screens.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwrtmanager.app.data.model.NetworkMode
import com.openwrtmanager.app.ui.common.AppStrings

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NetworkModesScreen(viewModel: NetworkModesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text(AppStrings.text("Network modes")) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(AppStrings.text("Choose a topology and configure this router"), style = MaterialTheme.typography.titleMedium)
            Text(
                AppStrings.text("A backup of network and wireless configuration is created before changes. Wi-Fi may disconnect during reload."),
                color = MaterialTheme.colorScheme.error
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ModeChip(state.mode == NetworkMode.WDS_BRIDGE, AppStrings.text("WDS Bridge")) { viewModel.selectMode(NetworkMode.WDS_BRIDGE) }
                ModeChip(state.mode == NetworkMode.MESH_80211S, AppStrings.text("Mesh 802.11s")) { viewModel.selectMode(NetworkMode.MESH_80211S) }
                ModeChip(state.mode == NetworkMode.REPEATER_RELAYD, AppStrings.text("Repeater")) { viewModel.selectMode(NetworkMode.REPEATER_RELAYD) }
            }
            Text(modeDescription(state.mode), style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(AppStrings.text("Install required packages"), style = MaterialTheme.typography.titleSmall)
                    Text(
                        AppStrings.text("Optional: install the LuCI/OpenWrt packages for this mode before applying configuration."),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = state.installPackages,
                    onCheckedChange = { enabled -> viewModel.update { copy(installPackages = enabled) } }
                )
            }

            when (state.mode) {
                NetworkMode.WDS_BRIDGE -> {
                    Field("Upstream SSID", state.upstreamSsid) { viewModel.update { copy(upstreamSsid = it) } }
                    PasswordField("Wi-Fi password", state.upstreamPassword) { viewModel.update { copy(upstreamPassword = it) } }
                }
                NetworkMode.MESH_80211S -> {
                    Field("Mesh ID", state.meshId) { viewModel.update { copy(meshId = it) } }
                    PasswordField("Mesh password", state.meshPassword) { viewModel.update { copy(meshPassword = it) } }
                }
                NetworkMode.REPEATER_RELAYD -> {
                    Field("Upstream SSID", state.upstreamSsid) { viewModel.update { copy(upstreamSsid = it) } }
                    PasswordField("Upstream password", state.upstreamPassword) { viewModel.update { copy(upstreamPassword = it) } }
                    Field("Local SSID", state.localSsid) { viewModel.update { copy(localSsid = it) } }
                    PasswordField("Local password", state.localPassword) { viewModel.update { copy(localPassword = it) } }
                    Field("Gateway IP", state.gatewayIp) { viewModel.update { copy(gatewayIp = it) } }
                }
            }
            Field("Wi-Fi channel (optional)", state.channel) { viewModel.update { copy(channel = it) } }
            Field("Router LAN IP", state.lanIp) { viewModel.update { copy(lanIp = it) } }

            Button(
                onClick = viewModel::requestApply,
                enabled = !state.running,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.running) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text(AppStrings.text("Apply network mode"))
            }
            state.resultMessage?.let {
                Card { Text(it, modifier = Modifier.padding(12.dp)) }
            }
            state.errorMessage?.let {
                Card { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
            }
        }
    }

    if (state.showConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::cancelApply,
            title = { Text(AppStrings.text("Apply network mode?")) },
            text = { Text(AppStrings.text(if (state.installPackages) "This backs up /etc/config/network and wireless, installs the selected packages, and reloads Wi-Fi/network. The active connection may be lost." else "This backs up /etc/config/network and wireless, applies UCI configuration without installing packages, and reloads Wi-Fi/network. The active connection may be lost.")) },
            confirmButton = { TextButton(onClick = viewModel::confirmApply) { Text(AppStrings.text("Apply"), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = viewModel::cancelApply) { Text(AppStrings.text("Cancel")) } }
        )
    }
}

@Composable
private fun ModeChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun Field(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(AppStrings.text(label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(AppStrings.text(label)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
}

private fun modeDescription(mode: NetworkMode): String = when (mode) {
    NetworkMode.WDS_BRIDGE -> AppStrings.text("For two or more fixed routers: the upstream AP must also support and enable WDS.")
    NetworkMode.MESH_80211S -> AppStrings.text("For a multi-path mesh: every node must use the same Mesh ID, password, channel, and compatible Wi-Fi security.")
    NetworkMode.REPEATER_RELAYD -> AppStrings.text("For an upstream router you do not control: relayd bridges a client connection to a local access point.")
}
