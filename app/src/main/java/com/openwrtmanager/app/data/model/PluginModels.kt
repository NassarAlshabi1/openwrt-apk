package com.openwrtmanager.app.data.model

/**
 * "Plugins"在 OpenWrt 生态里没有统一的形式 —— 大部分是 luci-app-xxx 这样的 opkg 包
 * （提供一个 LuCI 网页管理界面 + 一个后台服务），但具体交互千差万别（AdGuardHome、
 * PassWall、MosDNS、UPnP 等等 UI 和配置方式完全不同）。
 *
 * 当前Version策略：
 * 1. 通过Installed opkg 包列表，筛选出Package name匹配 "luci-app-*" / "luci-i18n-*"(排除) 等Rules的条目，
 *    作为"Plugins列表"展示。
 * 2. 点击进入通用详情页（占位），展示Package name/Version/Description等基础信息 + "Open in browser对应 LuCI 页面"
 *    的跳转按钮（因为大多数Plugins的管理页面本质是一个 LuCI controller 路径）。
 * 3. pluginKey 字段为未来"按Plugins类型适配专属 UI"预留 —— 后续可以维护一个
 *    known-plugin-registry（Package name -> 专属 Composable 路由），匹配不到则走通用兜底页面。
 */
data class InstalledPlugin(
    val packageName: String,       // 例如 "luci-app-adguardhome"
    val version: String,
    val description: String,
    // 猜测的 LuCI 管理路径，例如从 "luci-app-adguardhome" 推导出 "adguardhome"
    // 实际路径可能不准确（不同Plugins controller 命名不完全统一），仅作为"跳转到网页版"的辅助跳转。
    val guessedLuciPath: String?
)
