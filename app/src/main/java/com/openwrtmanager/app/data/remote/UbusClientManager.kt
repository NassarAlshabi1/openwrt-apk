package com.openwrtmanager.app.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 持有"当前活跃连接"的 UbusClient 实例。
 * App 同一时刻只操作一台路由器，切换路由器时替换这里的 client。
 * 用 Hilt Singleton 注入，保证全局唯一。
 */
@Singleton
class UbusClientManager @Inject constructor() {

    private val _activeClient = MutableStateFlow<UbusClient?>(null)
    val activeClient: StateFlow<UbusClient?> = _activeClient

    private val _activeProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = _activeProfileId

    fun setActive(profileId: String, client: UbusClient) {
        _activeClient.value = client
        _activeProfileId.value = profileId
    }

    fun clearActive() {
        _activeClient.value?.logout()
        _activeClient.value = null
        _activeProfileId.value = null
    }

    /** 抛异常而不是返回 null，方便 Repository 里用 requireClient() 简化空检查 */
    fun requireClient(): UbusClient =
        _activeClient.value ?: error("No active router connection. User must log in first.")
}
