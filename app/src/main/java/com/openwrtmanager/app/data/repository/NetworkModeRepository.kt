package com.openwrtmanager.app.data.repository

import com.openwrtmanager.app.data.model.NetworkMode
import com.openwrtmanager.app.data.model.NetworkModeConfig
import com.openwrtmanager.app.data.remote.UbusClientManager
import com.openwrtmanager.app.data.remote.UbusResult
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkModeRepository @Inject constructor(
    private val clientManager: UbusClientManager
) {
    suspend fun apply(config: NetworkModeConfig): Result<String> {
        validate(config).getOrElse { return Result.failure(it) }
        val backup = "/root/openwrt-manager-backup"
        val script = buildScript(config, backup)
        val params = buildJsonObject {
            put("command", JsonPrimitive("/bin/sh"))
            put("params", buildJsonArray {
                add(JsonPrimitive("-c"))
                add(JsonPrimitive(script))
            })
        }
        return when (val result = clientManager.requireClient().call("file", "exec", params)) {
            is UbusResult.Success -> {
                val data = result.data.jsonObject
                val code = data["code"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val stdout = data["stdout"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val stderr = data["stderr"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (code == 0) Result.success("Backup: $backup\n$stdout")
                else Result.failure(IllegalStateException(stderr.ifBlank { stdout.ifBlank { "Network configuration failed (exit code $code)" } }))
            }
            is UbusResult.UbusError -> Result.failure(IllegalStateException("ubus error ${result.code}: ${result.message}"))
            is UbusResult.NetworkError -> Result.failure(result.throwable)
        }
    }

    private fun validate(c: NetworkModeConfig): Result<Unit> {
        fun required(value: String, name: String): Result<Unit> =
            if (value.isBlank()) Result.failure(IllegalArgumentException("$name is required")) else Result.success(Unit)
        return when (c.mode) {
            NetworkMode.WDS_BRIDGE -> {
                required(c.upstreamSsid, "Upstream SSID").getOrElse { return Result.failure(it) }
                required(c.upstreamPassword, "Wi-Fi password").getOrElse { return Result.failure(it) }
            }
            NetworkMode.MESH_80211S -> {
                required(c.meshId, "Mesh ID").getOrElse { return Result.failure(it) }
                required(c.meshPassword, "Mesh password").getOrElse { return Result.failure(it) }
            }
            NetworkMode.REPEATER_RELAYD -> {
                required(c.upstreamSsid, "Upstream SSID").getOrElse { return Result.failure(it) }
                required(c.upstreamPassword, "Upstream password").getOrElse { return Result.failure(it) }
                required(c.localSsid, "Local SSID").getOrElse { return Result.failure(it) }
                required(c.localPassword, "Local password").getOrElse { return Result.failure(it) }
            }
        }
        return Result.success(Unit)
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private fun buildScript(c: NetworkModeConfig, backup: String): String {
        val packages = when (c.mode) {
            NetworkMode.WDS_BRIDGE -> "luci-app-wireless"
            NetworkMode.MESH_80211S -> "wpad-mesh-mbedtls luci-proto-batman-adv"
            NetworkMode.REPEATER_RELAYD -> "relayd luci-proto-relay"
        }
        val uci = when (c.mode) {
            NetworkMode.WDS_BRIDGE -> """
                uci set wireless.radio0.channel=${shellQuote(c.channel.ifBlank { "auto" })}
                uci set wireless.radio0.disabled='0'
                uci add wireless wifi-iface >/dev/null
                uci set wireless.@wifi-iface[-1].mode='sta'
                uci set wireless.@wifi-iface[-1].wds='1'
                uci set wireless.@wifi-iface[-1].ssid=${shellQuote(c.upstreamSsid)}
                uci set wireless.@wifi-iface[-1].encryption='psk2'
                uci set wireless.@wifi-iface[-1].key=${shellQuote(c.upstreamPassword)}
                uci set wireless.@wifi-iface[-1].network='lan'
            """.trimIndent()
            NetworkMode.MESH_80211S -> """
                uci set wireless.radio0.channel=${shellQuote(c.channel.ifBlank { "1" })}
                uci set wireless.radio0.disabled='0'
                uci add wireless wifi-iface >/dev/null
                uci set wireless.@wifi-iface[-1].mode='mesh'
                uci set wireless.@wifi-iface[-1].mesh_id=${shellQuote(c.meshId)}
                uci set wireless.@wifi-iface[-1].mesh_fwding='1'
                uci set wireless.@wifi-iface[-1].encryption='sae'
                uci set wireless.@wifi-iface[-1].key=${shellQuote(c.meshPassword)}
                uci set wireless.@wifi-iface[-1].network='lan'
            """.trimIndent()
            NetworkMode.REPEATER_RELAYD -> """
                uci set network.wwan='interface'
                uci set network.wwan.proto='dhcp'
                uci set network.stabridge='interface'
                uci set network.stabridge.proto='relay'
                uci set network.stabridge.network='lan wwan'
                uci set network.stabridge.ipaddr=${shellQuote(c.lanIp)}
                uci set network.stabridge.gateway=${shellQuote(c.gatewayIp)}
                uci add wireless wifi-iface >/dev/null
                uci set wireless.@wifi-iface[-1].mode='sta'
                uci set wireless.@wifi-iface[-1].ssid=${shellQuote(c.upstreamSsid)}
                uci set wireless.@wifi-iface[-1].encryption='psk2'
                uci set wireless.@wifi-iface[-1].key=${shellQuote(c.upstreamPassword)}
                uci set wireless.@wifi-iface[-1].network='wwan'
                uci add wireless wifi-iface >/dev/null
                uci set wireless.@wifi-iface[-1].mode='ap'
                uci set wireless.@wifi-iface[-1].ssid=${shellQuote(c.localSsid)}
                uci set wireless.@wifi-iface[-1].encryption='psk2'
                uci set wireless.@wifi-iface[-1].key=${shellQuote(c.localPassword)}
                uci set wireless.@wifi-iface[-1].network='lan'
            """.trimIndent()
        }
        return """
            set -eu
            backup=${shellQuote(backup)}
            mkdir -p "$backup"
            cp -f /etc/config/network /etc/config/wireless "$backup/"
            ${if (c.installPackages) "opkg update\n            opkg install $packages" else "# Package installation skipped by the user"}
            $uci
            uci commit wireless
            uci commit network
            /etc/init.d/network reload
            wifi reload || true
            echo "Configuration applied. Restore files from $backup if needed."
        """.trimIndent()
    }
}
