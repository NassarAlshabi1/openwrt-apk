package com.openwrtmanager.app.data.model

data class OpkgPackage(
    val name: String,
    val version: String,
    val installedVersion: String? = null,   // 非空表示Installed，且可能与 version（UpgradableVersion）不同
    val description: String = "",
    val sizeBytes: Long = 0L,
    val architecture: String = "",
    val isInstalled: Boolean = false,
    val isUpgradable: Boolean = false
)

/** opkg 长任务（update/install/remove/upgrade）的执行状态，供 UI 展示滚动日志和进度 */
sealed class OpkgTaskState {
    object Idle : OpkgTaskState()
    data class Running(val log: List<String>) : OpkgTaskState()
    data class Success(val log: List<String>, val summary: String) : OpkgTaskState()
    data class Failed(val log: List<String>, val error: String) : OpkgTaskState()
}
