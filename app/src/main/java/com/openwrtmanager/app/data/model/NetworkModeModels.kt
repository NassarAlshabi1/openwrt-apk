package com.openwrtmanager.app.data.model

enum class NetworkMode {
    WDS_BRIDGE,
    MESH_80211S,
    REPEATER_RELAYD
}

data class NetworkModeConfig(
    val mode: NetworkMode,
    val upstreamSsid: String = "",
    val upstreamPassword: String = "",
    val localSsid: String = "",
    val localPassword: String = "",
    val meshId: String = "",
    val meshPassword: String = "",
    val channel: String = "",
    val lanIp: String = "192.168.1.2",
    val gatewayIp: String = "192.168.1.1"
)
