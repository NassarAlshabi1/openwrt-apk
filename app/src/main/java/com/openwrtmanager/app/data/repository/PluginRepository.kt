package com.openwrtmanager.app.data.repository

import com.openwrtmanager.app.data.model.InstalledPlugin
import javax.inject.Inject
import javax.inject.Singleton

private val LUCI_APP_PREFIX = "luci-app-"

/**
 * Plugins列表 = Installed opkg 包中，Package name以 "luci-app-" 开头的那些
 * （这是 OpenWrt 生态里"有网页管理界面的Plugins"的事实标准命名约定）。
 *
 * 不包含 luci-i18n-*（纯语言包）、luci-theme-*（主题，不算功能Plugins）等噪音条目。
 */
@Singleton
class PluginRepository @Inject constructor(
    private val opkgRepository: OpkgRepository
) {
    suspend fun getInstalledPlugins(): Result<List<InstalledPlugin>> =
        opkgRepository.getInstalledPackages().map { packages ->
            packages
                .filter { it.name.startsWith(LUCI_APP_PREFIX) }
                .map { pkg ->
                    val shortName = pkg.name.removePrefix(LUCI_APP_PREFIX)
                    InstalledPlugin(
                        packageName = pkg.name,
                        version = pkg.installedVersion ?: pkg.version,
                        description = pkg.description,
                        guessedLuciPath = shortName.ifBlank { null }
                    )
                }
                .sortedBy { it.packageName }
        }
}
