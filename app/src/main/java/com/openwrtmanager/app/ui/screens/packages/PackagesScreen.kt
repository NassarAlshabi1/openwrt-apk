package com.openwrtmanager.app.ui.screens.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwrtmanager.app.data.model.OpkgPackage
import com.openwrtmanager.app.ui.common.FullScreenError
import com.openwrtmanager.app.ui.common.FullScreenLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesScreen(viewModel: PackagesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingRemove by remember { mutableStateOf<OpkgPackage?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Package management") },
                actions = {
                    IconButton(onClick = viewModel::runUpdate) {
                        Icon(Icons.Filled.Sync, contentDescription = "Update package index (opkg update)")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == PackagesTab.INSTALLED,
                    onClick = { viewModel.selectTab(PackagesTab.INSTALLED) },
                    text = { Text("Installed") }
                )
                Tab(
                    selected = uiState.selectedTab == PackagesTab.UPGRADABLE,
                    onClick = { viewModel.selectTab(PackagesTab.UPGRADABLE) },
                    text = { Text("Upgradable (${uiState.upgradable.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == PackagesTab.SEARCH,
                    onClick = { viewModel.selectTab(PackagesTab.SEARCH) },
                    text = { Text("Search and install") }
                )
            }

            when (uiState.selectedTab) {
                PackagesTab.INSTALLED -> PackageListContent(
                    isLoading = uiState.isLoadingList,
                    error = uiState.listErrorMessage,
                    packages = uiState.installed,
                    onRetry = { viewModel.selectTab(PackagesTab.INSTALLED) },
                    trailingAction = { pkg ->
                        IconButton(onClick = { pendingRemove = pkg }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Uninstall", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                PackagesTab.UPGRADABLE -> Column {
                    if (uiState.upgradable.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = viewModel::upgradeAll) {
                                Icon(Icons.Filled.Upgrade, contentDescription = null)
                                Text(" Upgrade all")
                            }
                        }
                    }
                    PackageListContent(
                        isLoading = uiState.isLoadingList,
                        error = uiState.listErrorMessage,
                        packages = uiState.upgradable,
                        onRetry = { viewModel.selectTab(PackagesTab.UPGRADABLE) },
                        trailingAction = { pkg ->
                            IconButton(onClick = { viewModel.upgradePackage(pkg.name) }) {
                                Icon(Icons.Filled.Upgrade, contentDescription = "Upgrade")
                            }
                        }
                    )
                }
                PackagesTab.SEARCH -> Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            label = { Text("Search packages") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = viewModel::search) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                    PackageListContent(
                        isLoading = uiState.isLoadingList,
                        error = uiState.listErrorMessage,
                        packages = uiState.searchResults,
                        onRetry = viewModel::search,
                        trailingAction = { pkg ->
                            IconButton(onClick = { viewModel.requestInstall(pkg) }) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = "Install")
                            }
                        }
                    )
                }
            }
        }

        if (uiState.taskPanelVisible) {
            OpkgTaskPanel(
                taskState = uiState.taskState,
                onDismiss = viewModel::dismissTaskPanel
            )
        }
    }

    // Installation confirmation（包含从 opkg info 拉取的详情 + 空间不足警告）
    uiState.pendingInstallTarget?.let { pkg ->
        val spaceWarning = viewModel.isPendingInstallSpaceLikelyInsufficient()
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingInstall,
            title = { Text("Install ${pkg.name}？") },
            text = {
                Column {
                    if (pkg.description.isNotBlank()) Text(pkg.description)
                    Text("Version: ${pkg.version}", style = MaterialTheme.typography.bodySmall)

                    if (uiState.isFetchingPendingInstallInfo) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text(" Loading package details…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                        }
                    } else if (uiState.pendingInstallInfo != null) {
                        Text(
                            uiState.pendingInstallInfo ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (spaceWarning) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                "Router storage may be insufficient; installation could fail",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmPendingInstall) { Text("Install") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPendingInstall) { Text("Cancel") }
            }
        )
    }

    // Uninstall确认
    pendingRemove?.let { pkg ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Uninstall ${pkg.name}？") },
            text = { Text("Removing core packages (such as dropbear or uhttpd) may make the router unmanageable. Proceed with caution.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removePackage(pkg.name)
                    pendingRemove = null
                }) { Text("Uninstall", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PackageListContent(
    isLoading: Boolean,
    error: String?,
    packages: List<OpkgPackage>,
    onRetry: () -> Unit,
    trailingAction: @Composable (OpkgPackage) -> Unit
) {
    when {
        isLoading -> FullScreenLoading()
        error != null -> FullScreenError(error, onRetry = onRetry)
        packages.isEmpty() -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No data", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(packages, key = { it.name }) { pkg ->
                ListItem(
                    headlineContent = { Text(pkg.name) },
                    supportingContent = {
                        Column {
                            if (pkg.description.isNotBlank()) {
                                Text(pkg.description, maxLines = 2)
                            }
                            Text(
                                if (pkg.isInstalled) "Installed version: ${pkg.installedVersion ?: pkg.version}"
                                else "Version: ${pkg.version}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    trailingContent = { trailingAction(pkg) }
                )
            }
        }
    }
}
