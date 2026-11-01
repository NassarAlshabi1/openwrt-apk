package com.openwrtmanager.app.data.model

import kotlinx.serialization.Serializable

/**
 * 一个已保存的路由器连接配置。
 * Password不放在这里明文存储——Password单独通过 EncryptedSharedPreferences 按 id 存取，
 * 这个 model 只负责非敏感的连接元数据，方便整体做 JSON 序列化保存到 DataStore。
 */
@Serializable
data class RouterProfile(
    val id: String,               // UUID，作为主键，也用作Password在加密存储中的 key
    val name: String,             // 用户自定义名称，例如 "家里路由器"
    val baseUrl: String,          // 例如 http://192.168.1.1 或 https://192.168.1.1:443
    val username: String,
    val trustSelfSignedCert: Boolean = false,
    val lastConnectedAt: Long = 0L
)
