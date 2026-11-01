# OpenWrt Manager (Android)

一个原生 Android (Kotlin + Jetpack Compose) 应用，通过 OpenWrt 的 `ubus` JSON-RPC 接口
（与官方 LuCI 网页版使用的是同一套底层协议）直接管理路由器：系统状态、软件包（opkg）、
插件、防火墙、DHCP。

面向 **OpenWrt 24.10+**。

## 编译

1. 用 Android Studio（Koala 或更新版本）打开本项目根目录。
2. Gradle Sync 会自动拉取依赖（Kotlin 1.9.24 / AGP 8.5.2 / compileSdk 34 / minSdk 29）。
3. 直接 Run，或 `./gradlew assembleDebug` 生成 APK。

> 注意：本仓库未包含 `gradle/wrapper/gradle-wrapper.jar` 二进制文件（生成环境无法访问
> Gradle 官方下载源）。用 Android Studio 打开项目时会自动下载补全；如果你想直接用命令行
> `./gradlew`，先执行一次 `gradle wrapper --gradle-version 8.7`（需要本地已安装 Gradle）
> 生成该文件，或者直接用 Android Studio 的 Gradle Sync 触发下载。

无需任何服务端改动，路由器侧不需要额外安装插件——ubus/rpcd 是 OpenWrt 默认自带的组件。

## 路由器侧的前置条件

- 确保能通过局域网访问路由器的 LuCI 网页（说明 uhttpd + rpcd 都在正常运行）。
- 使用你平时登录 LuCI 网页版的用户名密码（通常是 `root`）。
- **重要**：软件包管理（opkg 相关功能）依赖该账户在 ACL 中被允许调用 `file` 对象的
  `exec` 方法。默认的 root 账户（`luci-mod-admin-full` 全权限 ACL）已经包含这个权限。
  如果你自定义了更严格的 ACL、锁死了普通用户权限，opkg 相关功期会请求失败并给出错误提示，
  此时需要到 `/usr/share/rpcd/acl.d/` 下调整该账户的权限组。

## 已实现功能

- **登录**：输入路由器地址 + 用户名密码，本地加密保存（Android Keystore），支持保存多个路由器。
- **概览 (Dashboard)**：主机名、固件版本、运行时间、负载、内存/存储占用，重启路由器（二次确认）。
- **软件包管理 (Packages)**：已安装列表、可更新列表（一键全部更新）、关键字搜索安装、
  安装前自动查询 `opkg info` 详情与体积、存储空间不足预警、操作过程滚动日志面板。
- **插件 (Plugins)**：列出所有 `luci-app-*` 包作为"插件"，点击进入通用详情页
  （当前是占位页面，展示基础信息 + 跳转到该插件在 LuCI 网页版的管理页面）。
  已预留按包名适配专属插件 UI 的扩展点，见 `PluginDetailScreen.kt` 顶部注释。
- **防火墙 (Firewall)**：查看区域(Zone)、管理规则(Rule)（增/删/启用禁用），改动需要显式点击
  "应用改动"才会 commit + reload 到实际生效（对应真实 LuCI 的"未保存的改动"机制）。
- **DHCP**：DHCP 池启停、静态租约(MAC-IP 绑定)增删。

## 架构

```
data/
  remote/       UbusClient（ubus JSON-RPC 协议实现）、UbusClientManager（当前活跃连接）
  local/        RouterProfileStore（DataStore，连接元数据）、SecureCredentialStore（Keystore 加密密码）
  model/        领域模型（SystemStatus / UciSection / FirewallRule / DhcpPool / OpkgPackage / InstalledPlugin ...）
  repository/   SessionRepository / SystemRepository / UciRepository / FirewallRepository /
                DhcpRepository / OpkgRepository / PluginRepository
ui/
  screens/      各功能页面的 Composable + ViewModel（MVVM）
  common/       共享 UI 组件（底部导航栏、Loading/Error 占位）
  theme/        Material3 主题
di/             Hilt 模块
```

核心设计原则：

- **防火墙 / DHCP 本质是同一个 UCI 配置系统**，两者共用 `UciRepository` 的通用读写能力，
  上层只需提供 schema 解析（`FirewallRepository` / `DhcpRepository`）。
- **软件包管理走 `file.exec` 直接调用 `opkg` 命令行**并解析标准输出，这是目前对新版本
  OpenWrt 兼容性最好的方式（不依赖已经过时的 `luci.opkg` 高层封装对象）。
- **插件列表是 opkg 已安装包的一个视图**（筛选 `luci-app-*` 前缀），不是独立的数据源。

## 已知限制 / 后续可扩展方向

- opkg 长任务（update/install/remove/upgrade）目前是"调用完成后一次性展示完整日志"，
  不是真正的逐行实时流（ubus 的 `file.exec` 本身是同步阻塞调用）。
- 插件详情页目前只有通用占位实现；后续可以为常见插件（AdGuard Home、PassWall、MosDNS 等）
  建立"包名 → 专属 Composable"的映射表，命中时展示专属管理界面。
- 防火墙/DHCP 的 UCI 字段解析覆盖了最常用的字段，个别高级选项（例如复杂的
  IPv6 相关配置）可能需要按需扩展 `UciSection` 的解析逻辑。
- 默认信任局域网明文 HTTP；如果路由器配置了自签 HTTPS 证书，目前依赖 Android 系统的
  用户信任证书存储（需要用户手动导入证书），尚未做应用内的证书指纹确认 UI。

## GitHub Actions release

The `Build and publish APK` workflow builds the unsigned `release` variant and publishes the APK and SHA-256 checksum to GitHub Releases. Run it manually from the Actions tab, or push a tag matching `v*`. The Android UI is provided in English.
