package com.openwrtmanager.app.data.repository

import com.openwrtmanager.app.data.model.UciConfig
import com.openwrtmanager.app.data.model.UciSection
import com.openwrtmanager.app.data.remote.UbusClientManager
import com.openwrtmanager.app.data.remote.UbusResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UCI 是防火墙、DHCP、网络配置的统一底座。
 * 所有配置类功能（Firewall/DHCP UI）都通过这一个 Repository 读写，
 * 上层只需要提供 configName（"firewall" / "dhcp" / "network"）。
 */
@Singleton
class UciRepository @Inject constructor(
    private val clientManager: UbusClientManager
) {
    /** 读取整个 config（例如 "firewall"），解析为通用 section 列表 */
    suspend fun getConfig(configName: String): Result<UciConfig> {
        val client = clientManager.requireClient()
        val params = buildJsonObject { put("config", JsonPrimitive(configName)) }
        return when (val result = client.call("uci", "get", params)) {
            is UbusResult.Success -> {
                val values = result.data.jsonObject["values"]?.jsonObject ?: JsonObject(emptyMap())
                val sections = values.entries.map { (sectionId, elem) ->
                    parseSection(sectionId, elem.jsonObject)
                }
                Result.success(UciConfig(configName, sections))
            }
            else -> Result.failure(IllegalStateException(errorMessage(result)))
        }
    }

    private fun parseSection(sectionId: String, obj: JsonObject): UciSection {
        val type = obj[".type"]?.jsonPrimitive?.contentOrNull ?: "unknown"
        val options = mutableMapOf<String, String>()
        val listOptions = mutableMapOf<String, List<String>>()
        for ((key, value) in obj) {
            if (key.startsWith(".")) continue // 跳过元字段 .type/.name/.anonymous
            when (value) {
                is JsonArray -> listOptions[key] = value.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" }
                else -> options[key] = value.jsonPrimitive.contentOrNull ?: ""
            }
        }
        return UciSection(sectionId, type, options, listOptions)
    }

    /**
     * 新建一个 section（例如新增一条防火墙规则或一条 DHCP 静态租约）。
     * 返回新 section 的 id。注意：uci add 只是写入"未提交"的内存态，需要调用 commit() 落盘。
     */
    suspend fun addSection(
        configName: String,
        type: String,
        values: Map<String, String> = emptyMap()
    ): Result<String> {
        val client = clientManager.requireClient()
        val params = buildJsonObject {
            put("config", JsonPrimitive(configName))
            put("type", JsonPrimitive(type))
            put("values", buildJsonObject {
                values.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
            })
        }
        return when (val result = client.call("uci", "add", params)) {
            is UbusResult.Success -> {
                val sectionId = result.data.jsonObject["section"]?.jsonPrimitive?.contentOrNull
                    ?: return Result.failure(IllegalStateException("uci add did not return a section id"))
                Result.success(sectionId)
            }
            else -> Result.failure(IllegalStateException(errorMessage(result)))
        }
    }

    /** 修改指定 section 的若干个 option（同样是内存态，需要 commit） */
    suspend fun setValues(
        configName: String,
        sectionId: String,
        values: Map<String, String>
    ): Result<Unit> {
        val client = clientManager.requireClient()
        val params = buildJsonObject {
            put("config", JsonPrimitive(configName))
            put("section", JsonPrimitive(sectionId))
            put("values", buildJsonObject {
                values.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
            })
        }
        return when (val result = client.call("uci", "set", params)) {
            is UbusResult.Success -> Result.success(Unit)
            else -> Result.failure(IllegalStateException(errorMessage(result)))
        }
    }

    /** 删除一个 section（例如删除一条防火墙规则） */
    suspend fun deleteSection(configName: String, sectionId: String): Result<Unit> {
        val client = clientManager.requireClient()
        val params = buildJsonObject {
            put("config", JsonPrimitive(configName))
            put("section", JsonPrimitive(sectionId))
        }
        return when (val result = client.call("uci", "delete", params)) {
            is UbusResult.Success -> Result.success(Unit)
            else -> Result.failure(IllegalStateException(errorMessage(result)))
        }
    }

    /** 提交内存态改动到磁盘（/etc/config/<name>） */
    suspend fun commit(configName: String): Result<Unit> {
        val client = clientManager.requireClient()
        val params = buildJsonObject { put("config", JsonPrimitive(configName)) }
        return when (val result = client.call("uci", "commit", params)) {
            is UbusResult.Success -> Result.success(Unit)
            else -> Result.failure(IllegalStateException(errorMessage(result)))
        }
    }

    /** 放弃内存态改动 */
    suspend fun revert(configName: String): Result<Unit> {
        val client = clientManager.requireClient()
        val params = buildJsonObject { put("config", JsonPrimitive(configName)) }
        return when (val result = client.call("uci", "revert", params)) {
            is UbusResult.Success -> Result.success(Unit)
            else -> Result.failure(IllegalStateException(errorMessage(result)))
        }
    }

    /**
     * 让配置改动实际生效（不仅仅是写入 /etc/config/ 下的文件，还要让运行中的服务重新加载）。
     * 对 firewall 是 `/etc/init.d/firewall reload`，对 dhcp/network 通常是重启 dnsmasq / netifd。
     * 通过 luci 提供的 "luci" -> "reload_config" 或直接调用 file/exec 类接口。
     * 这里统一走 rpcd 的 "file" exec 能力（24.10 默认开启 file 对象，ACL 允许时可用）。
     */
    suspend fun applyChanges(service: String): Result<Unit> {
        val client = clientManager.requireClient()
        val params = buildJsonObject {
            put("command", JsonPrimitive("/etc/init.d/$service"))
            put("params", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive("reload")) })
        }
        return when (val result = client.call("file", "exec", params)) {
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
