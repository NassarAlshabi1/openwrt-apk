package com.openwrtmanager.app.data.repository

import com.openwrtmanager.app.data.model.OpkgPackage
import com.openwrtmanager.app.data.model.OpkgTaskState
import com.openwrtmanager.app.data.remote.UbusClientManager
import com.openwrtmanager.app.data.remote.UbusResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Package management。
 *
 * 24.10+ 的 rpcd Default没有专门的 "luci.opkg" 高层封装对象（那是老Version LuCI2 的产物），
 * 现代 LuCI (JS 版) 走的是更底层的 "file" exec 能力直接调用 opkg 命令行，
 * 再解析标准输出。这是目前对新版 OpenWrt 最稳妥、兼容性最好的做法：
 *   opkg update
 *   opkg list / opkg list-installed / opkg list-upgradable
 *   opkg install <pkg> / opkg remove <pkg>
 *
 * 注意：这要求 ACL 中该用户被允许调用 "file" 的 "exec" 方法（Default root 权限的
 * luci-mod-admin-full ACL 已包含）。如果目标路由器锁得很死，需要引导用户放开 ACL。
 */
@Singleton
class OpkgRepository @Inject constructor(
    private val clientManager: UbusClientManager
) {

    /** 执行 opkg update，流式Back日志行，便于 UI 做滚动日志展示 */
    fun update(): Flow<OpkgTaskState> = execOpkgStreaming(listOf("update"))

    /** Search/列出可用包。keyword 为空则Back全部（数据量可能很大，UI 层应做分页/懒加载） */
    suspend fun findPackages(keyword: String): Result<List<OpkgPackage>> {
        val args = if (keyword.isBlank()) listOf("list") else listOf("list", keyword)
        return execOpkgOnce(args).map { output -> parsePackageListOutput(output, installedOnly = false) }
    }

    suspend fun getInstalledPackages(): Result<List<OpkgPackage>> =
        execOpkgOnce(listOf("list-installed")).map { output ->
            parsePackageListOutput(output, installedOnly = true)
        }

    suspend fun getUpgradablePackages(): Result<List<OpkgPackage>> =
        execOpkgOnce(listOf("list-upgradable")).map { output ->
            parsePackageListOutput(output, installedOnly = false)
        }

    /** 包详情（用于Install前预览：说明、依赖、体积） */
    suspend fun getPackageInfo(name: String): Result<String> =
        execOpkgOnce(listOf("info", name))

    /**
     * 从 `opkg info` 的输出中解析 "Installed-Size" 字段（单位字节）。
     * 典型输出行： "Installed-Size: 12345"
     * 解析不到时Back null，调用方应把"无法判断体积"当作一种正常情况处理（不报警），
     * 而不是当成 0 字节。
     */
    fun parseInstalledSizeBytes(opkgInfoOutput: String): Long? =
        opkgInfoOutput.lineSequence()
            .firstOrNull { it.trim().startsWith("Installed-Size", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.toLongOrNull()

    fun install(packageName: String): Flow<OpkgTaskState> = execOpkgStreaming(listOf("install", packageName))

    fun remove(packageName: String): Flow<OpkgTaskState> = execOpkgStreaming(listOf("remove", packageName))

    fun upgrade(packageName: String): Flow<OpkgTaskState> = execOpkgStreaming(listOf("upgrade", packageName))

    fun upgradeAll(packageNames: List<String>): Flow<OpkgTaskState> = flow {
        emit(OpkgTaskState.Running(emptyList()))
        val log = mutableListOf<String>()
        for (pkg in packageNames) {
            log.add("=== upgrading $pkg ===")
            emit(OpkgTaskState.Running(log.toList()))
            val result = execOpkgOnce(listOf("upgrade", pkg))
            result.onSuccess { output ->
                log.addAll(output.lines())
            }.onFailure { err ->
                log.add("ERROR: ${err.message}")
                emit(OpkgTaskState.Failed(log.toList(), err.message ?: "upgrade failed on $pkg"))
                return@flow
            }
        }
        emit(OpkgTaskState.Success(log.toList(), "All ${packageNames.size} packages upgraded"))
    }

    // ---- internal exec helpers ----

    /** 单次执行并等待完整输出（适合列表/查询类，输出量可控） */
    private suspend fun execOpkgOnce(args: List<String>): Result<String> {
        val client = clientManager.requireClient()
        val params = buildJsonObject {
            put("command", JsonPrimitive("opkg"))
            put("params", buildJsonArray { args.forEach { add(JsonPrimitive(it)) } })
        }
        return when (val result = client.call("file", "exec", params)) {
            is UbusResult.Success -> {
                val stdout = result.data.jsonObject["stdout"]?.jsonPrimitive?.contentOrNull ?: ""
                val code = result.data.jsonObject["code"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                if (code != 0) {
                    val stderr = result.data.jsonObject["stderr"]?.jsonPrimitive?.contentOrNull ?: ""
                    Result.failure(IllegalStateException(stderr.ifBlank { "opkg exited with code $code" }))
                } else {
                    Result.success(stdout)
                }
            }
            is UbusResult.UbusError -> Result.failure(IllegalStateException("ubus error ${result.code}: ${result.message}"))
            is UbusResult.NetworkError -> Result.failure(result.throwable)
        }
    }

    /**
     * 流式执行长任务（update/install/remove/upgrade）。
     * ubus 的 file.exec 本身是同步阻塞调用（不是真正的流），所以这里用"先Back Running 占位，
     * 调用完成后一次性给出完整日志"的方式模拟——对 UI 已经足够（比完全没有反馈好很多）。
     * 如果未来 rpcd Version支持 exec 的异步流（例如通过 ubus subscribe / procd 事件），
     * 可以在这里替换为真正的逐行 emit 而不改动上层调用方。
     */
    private fun execOpkgStreaming(args: List<String>): Flow<OpkgTaskState> = flow {
        emit(OpkgTaskState.Running(listOf("$ opkg ${args.joinToString(" ")}")))
        val result = execOpkgOnce(args)
        result.onSuccess { output ->
            val log = listOf("$ opkg ${args.joinToString(" ")}") + output.lines().filter { it.isNotBlank() }
            emit(OpkgTaskState.Success(log, "Command completed"))
        }.onFailure { err ->
            emit(OpkgTaskState.Failed(listOf("$ opkg ${args.joinToString(" ")}"), err.message ?: "Operation failed"))
        }
    }

    /**
     * 解析 `opkg list` / `opkg list-installed` / `opkg list-upgradable` 的输出。
     * 典型行格式：
     *   list:            "pkgname - version - description"
     *   list-installed:  "pkgname - version"
     *   list-upgradable: "pkgname - old_version - new_version"
     *
     * 注意：opkg list 系列命令不直接输出包体积，sizeBytes 这里保持为 0。
     * 如果需要展示体积（Install前空间检查用），要对候选包再调用一次 `opkg info <pkg>`
     * 解析其中的 "Installed-Size" 字段——因为对每一条列表结果都单独 info 查询开销较大，
     * 目前只在用户点开"Install确认对话框"时才按需查询单个包的详情（见 getPackageInfo）。
     */
    private fun parsePackageListOutput(output: String, installedOnly: Boolean): List<OpkgPackage> {
        return output.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(" - ", limit = 3)
                if (parts.isEmpty()) return@mapNotNull null
                val name = parts.getOrNull(0)?.trim() ?: return@mapNotNull null
                val version = parts.getOrNull(1)?.trim() ?: ""
                val description = parts.getOrNull(2)?.trim() ?: ""
                OpkgPackage(
                    name = name,
                    version = version,
                    installedVersion = if (installedOnly) version else null,
                    description = description,
                    isInstalled = installedOnly,
                    isUpgradable = false
                )
            }
            .toList()
    }
}
