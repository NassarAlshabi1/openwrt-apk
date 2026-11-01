package com.openwrtmanager.app.ui.screens.plugins
import com.openwrtmanager.app.ui.common.AppStrings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openwrtmanager.app.data.model.InstalledPlugin

/**
 * Plugins详情页 —— 当前Version是"通用兜底页面"。
 *
 * 扩展点说明（后续如何为具体Plugins适配专属 UI）：
 * 在 PluginDetailRoute 里维护一个 `packageName -> @Composable 专属页面` 的映射表
 * （类似一个小型 registry），匹配到已知Plugins（如 luci-app-adguardhome）时跳转到专属页面，
 * 匹配不到时走这个通用兜底页面。这样Add一个Plugins适配不需要改动列表/路由逻辑，
 * 只需要在 registry 里加一条映射 + 实现对应 Composable。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    plugin: InstalledPlugin,
    baseUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(plugin.packageName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = AppStrings.text("Back"))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Extension,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(AppStrings.text("Package name"), style = MaterialTheme.typography.labelMedium)
                    Text(plugin.packageName, style = MaterialTheme.typography.bodyLarge)

                    Text(
                        AppStrings.text("Version"),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(plugin.version, style = MaterialTheme.typography.bodyLarge)

                    if (plugin.description.isNotBlank()) {
                        Text(
                            AppStrings.text("Description"),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(plugin.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text(
                "This plugin does not have a dedicated management screen yet. Open its LuCI web interface in a browser.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (plugin.guessedLuciPath != null) {
                Button(onClick = {
                    val url = "$baseUrl/cgi-bin/luci/admin/services/${plugin.guessedLuciPath}"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                    Text(" Open in browser")
                }
            }
        }
    }
}
