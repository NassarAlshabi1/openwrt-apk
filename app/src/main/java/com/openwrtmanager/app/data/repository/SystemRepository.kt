package com.openwrtmanager.app.data.repository

import com.openwrtmanager.app.data.model.SystemStatus
import com.openwrtmanager.app.data.remote.UbusClientManager
import com.openwrtmanager.app.data.remote.UbusResult
import com.openwrtmanager.app.data.remote.stringOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统状态相关：对应 ubus 的 "system" 对象（board / info）。
 * 24.10+ 上这两个方法是最通用、最稳定的系统信息来源。
 * 磁盘使用信息（diskusage）不是所有固件都暴露在 system.info 里，缺失时保持为 0，
 * UI 层会据此隐藏"存储空间"卡片。
 */
@Singleton
class SystemRepository @Inject constructor(
    private val clientManager: UbusClientManager
) {
    suspend fun getSystemStatus(): Result<SystemStatus> {
        val client = clientManager.requireClient()

        val boardResult = client.call("system", "board")
        val infoResult = client.call("system", "info")

        val board = (boardResult as? UbusResult.Success)?.data?.jsonObject
            ?: return Result.failure(IllegalStateException(errorMessage(boardResult)))
        val info = (infoResult as? UbusResult.Success)?.data?.jsonObject
            ?: return Result.failure(IllegalStateException(errorMessage(infoResult)))

        val memory = info["memory"]?.jsonObject
        val diskUsage = info["diskusage"]?.jsonObject // 部分固件才有；缺失时磁盘相关字段保持 0

        val loadRaw = info["load"]?.jsonArray
        // ubus system.info 的 load 是定点数（乘以 65536），需要还原成常见的 loadavg 浮点表示
        val load = loadRaw?.map { (it.jsonPrimitive.longOrNull ?: 0L) / 65536.0 } ?: listOf(0.0, 0.0, 0.0)

        val status = SystemStatus(
            hostname = board.stringOrNull("hostname") ?: "OpenWrt",
            model = board.stringOrNull("model") ?: "Unknown device",
            firmwareVersion = board["release"]?.jsonObject?.stringOrNull("description") ?: "Unknown",
            kernelVersion = board.stringOrNull("kernel") ?: "-",
            uptimeSeconds = info["uptime"]?.jsonPrimitive?.longOrNull ?: 0L,
            loadAverage = load,
            memoryTotalKb = (memory?.get("total")?.jsonPrimitive?.longOrNull ?: 0L) / 1024,
            memoryFreeKb = (memory?.get("free")?.jsonPrimitive?.longOrNull ?: 0L) / 1024,
            rootfsTotalKb = diskUsage?.get("root")?.jsonObject?.get("total")?.jsonPrimitive?.longOrNull ?: 0L,
            rootfsFreeKb = diskUsage?.get("root")?.jsonObject?.get("free")?.jsonPrimitive?.longOrNull ?: 0L
        )
        return Result.success(status)
    }

    /** 危险操作：重启路由器。UI 层必须做二次确认。 */
    suspend fun reboot(): Result<Unit> {
        val client = clientManager.requireClient()
        return when (val result = client.call("system", "reboot")) {
            is UbusResult.Success -> Result.success(Unit)
            else -> Result.failure(IllegalStateException(errorMessage(result)))
        }
    }

    private fun errorMessage(result: UbusResult): String = when (result) {
        is UbusResult.UbusError -> "ubus error ${result.code}: ${result.message}"
        is UbusResult.NetworkError -> result.throwable.message ?: "network error"
        else -> "unknown error"
    }
}
