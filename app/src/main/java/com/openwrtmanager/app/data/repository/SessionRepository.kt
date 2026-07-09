package com.openwrtmanager.app.data.repository

import com.openwrtmanager.app.data.remote.UbusClient
import com.openwrtmanager.app.data.remote.UbusClientManager
import com.openwrtmanager.app.data.remote.UbusResult
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

sealed class LoginOutcome {
    object Success : LoginOutcome()
    data class WrongCredentials(val message: String) : LoginOutcome()
    data class NetworkFailure(val message: String) : LoginOutcome()
    data class OtherError(val message: String) : LoginOutcome()
}

/**
 * 负责登录/登出，以及登录后建立的 UbusClient 与 UbusClientManager 的绑定。
 */
@Singleton
class SessionRepository @Inject constructor(
    private val clientManager: UbusClientManager,
    private val sharedHttpClient: OkHttpClient
) {
    suspend fun login(profileId: String, baseUrl: String, username: String, password: String): LoginOutcome {
        val client = UbusClient(baseUrl = baseUrl.trimEnd('/'), okHttpClient = sharedHttpClient)
        return when (val result = client.login(username, password)) {
            is UbusResult.Success -> {
                clientManager.setActive(profileId, client)
                LoginOutcome.Success
            }
            is UbusResult.UbusError -> {
                if (result.code == 6) {
                    LoginOutcome.WrongCredentials("用户名或密码错误")
                } else {
                    LoginOutcome.OtherError(result.message)
                }
            }
            is UbusResult.NetworkError -> LoginOutcome.NetworkFailure(
                result.throwable.message ?: "网络连接失败，请检查地址和网络"
            )
        }
    }

    fun logout() {
        clientManager.clearActive()
    }

    /**
     * 探测当前用户在各个 ubus object 上有哪些方法权限。
     * 对应 `session access` / ACL 读取，用于功能级降级（缺权限就隐藏对应模块，而不是崩溃）。
     * 简化实现：尝试列出可用对象（"list" 方法在 rpcd 上通常允许探测）。
     */
    suspend fun probeCapabilities(): Set<String> {
        val client = clientManager.activeClient.value ?: return emptySet()
        val result = client.call("session", "access", JsonObject(emptyMap()))
        return if (result is UbusResult.Success) {
            result.data.let { it as? JsonObject }?.keys ?: emptySet()
        } else {
            emptySet()
        }
    }
}
