package com.openwrtmanager.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openwrtmanager.app.data.remote.UbusClientManager
import com.openwrtmanager.app.ui.common.AppBottomBar
import com.openwrtmanager.app.ui.screens.dashboard.DashboardScreen
import com.openwrtmanager.app.ui.screens.dhcp.DhcpScreen
import com.openwrtmanager.app.ui.screens.firewall.FirewallScreen
import com.openwrtmanager.app.ui.screens.login.LoginScreen
import com.openwrtmanager.app.ui.screens.packages.PackagesScreen
import com.openwrtmanager.app.ui.screens.plugins.PluginDetailScreen
import com.openwrtmanager.app.ui.screens.plugins.PluginsScreen
import com.openwrtmanager.app.ui.screens.plugins.PluginsViewModel
import com.openwrtmanager.app.ui.screens.network.NetworkModesScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavHost(
    ubusClientManager: UbusClientManager
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            MainScaffold(navController) {
                DashboardScreen(onOpenNetworkModes = {
                    navController.navigate(Routes.NETWORK_MODES)
                }, onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
        }

        composable(Routes.PACKAGES) {
            MainScaffold(navController) { PackagesScreen() }
        }

        composable(Routes.PLUGINS) {
            // 共享 PluginsViewModel 实例（用同一个 nav graph entry 的 viewModel），
            // 这样点击列表项跳转到详情页时可以直接按Package name从已加载的列表里查找，
            // 不需要重复请求或做复杂的参数序列化。
            val pluginsViewModel: PluginsViewModel = hiltViewModel()
            MainScaffold(navController) {
                PluginsScreen(
                    viewModel = pluginsViewModel,
                    onPluginClick = { plugin ->
                        val encodedName = URLEncoder.encode(plugin.packageName, "UTF-8")
                        navController.navigate(Routes.pluginDetail(encodedName))
                    }
                )
            }
        }

        composable(
            route = Routes.PLUGIN_DETAIL,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("packageName") ?: ""
            val packageName = URLDecoder.decode(encodedName, "UTF-8")

            // 从当前活跃连接的 baseUrl 拼跳转链接；从共享的 PluginsViewModel 状态里按Package name查找详情。
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.PLUGINS)
            }
            val pluginsViewModel: PluginsViewModel = hiltViewModel(parentEntry)
            val uiState by pluginsViewModel.uiState.collectAsState()
            val plugin = uiState.plugins.firstOrNull { it.packageName == packageName }
            val baseUrl = ubusClientManager.activeClient.collectAsState().value?.baseUrl ?: ""

            if (plugin != null) {
                PluginDetailScreen(
                    plugin = plugin,
                    baseUrl = baseUrl,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.FIREWALL) {
            MainScaffold(navController) { FirewallScreen() }
        }

        composable(Routes.DHCP) {
            MainScaffold(navController) { DhcpScreen() }
        }

        composable(Routes.NETWORK_MODES) {
            MainScaffold(navController) { NetworkModesScreen() }
        }
    }
}

@Composable
private fun MainScaffold(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            content()
        }
    }
}
