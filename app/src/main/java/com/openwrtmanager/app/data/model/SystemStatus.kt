package com.openwrtmanager.app.data.model

data class SystemStatus(
    val hostname: String,
    val model: String,
    val firmwareVersion: String,
    val kernelVersion: String,
    val uptimeSeconds: Long,
    val loadAverage: List<Double>,   // 1/5/15 分钟负载
    val memoryTotalKb: Long,
    val memoryFreeKb: Long,
    val rootfsTotalKb: Long,         // 用于 opkg 安装前的空间检查
    val rootfsFreeKb: Long
) {
    val memoryUsedPercent: Int
        get() = if (memoryTotalKb <= 0) 0
        else (((memoryTotalKb - memoryFreeKb).toDouble() / memoryTotalKb) * 100).toInt()

    val rootfsUsedPercent: Int
        get() = if (rootfsTotalKb <= 0) 0
        else (((rootfsTotalKb - rootfsFreeKb).toDouble() / rootfsTotalKb) * 100).toInt()
}
