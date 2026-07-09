package com.openwrtmanager.app.ui

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val PACKAGES = "packages"
    const val PLUGINS = "plugins"
    const val PLUGIN_DETAIL = "plugin_detail/{packageName}"
    const val FIREWALL = "firewall"
    const val DHCP = "dhcp"
    const val SETTINGS = "settings"

    fun pluginDetail(packageName: String) = "plugin_detail/$packageName"
}
