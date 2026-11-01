package com.openwrtmanager.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val EMPTY_SESSION = "00000000000000000000000000000000"
private val JSON_MEDIA_TYPE = "application/json".toMediaType()

/**
 * ubus JSON-RPC over HTTP 客户端。
 *
 * OpenWrt 的 /ubus endpoint 接受这样的请求体：
 * {
 *   "jsonrpc": "2.0", "id": 1, "method": "call",
 *   "params": [<session_id>, <object>, <method>, {<args>}]
 * }
 *
 * 一个 UbusClient 实例对应一台路由器的一条连接。session 状态保存在实例内部，
 * 上层 Repository 只需要调用 call(obj, method, params) 而不用关心 token 管理。
 */
class UbusClient(
    val baseUrl: String,
    private val okHttpClient: OkHttpClient = defaultHttpClient()
) {
    @Volatile
    private var sessionId: String = EMPTY_SESSION

    @Volatile
    private var username: String? = null

    @Volatile
    private var password: String? = null

    private val requestIdCounter = AtomicInteger(1)

    val isLoggedIn: Boolean
        get() = sessionId != EMPTY_SESSION

    /** Sign in，成功后内部保存 session，供后续 call() 自动使用 */
    suspend fun login(user: String, pass: String): UbusResult {
        val result = rawCall(
            session = EMPTY_SESSION,
            obj = "session",
            method = "login",
            params = buildJsonObject {
                put("username", JsonPrimitive(user))
                put("password", JsonPrimitive(pass))
            }
        )
        if (result is UbusResult.Success) {
            val ubusRpcSession = result.data.jsonObject["ubus_rpc_session"]?.jsonPrimitive?.contentOrNull
            if (ubusRpcSession != null) {
                sessionId = ubusRpcSession
                username = user
                password = pass
            }
        }
        return result
    }

    fun logout() {
        sessionId = EMPTY_SESSION
        username = null
        password = null
    }

    /**
     * 通用调用入口。所有业务 Repository 都通过这个方法与路由器交互。
     * 如果遇到 PERMISSION_DENIED（session 过期），会自动尝试用保存的凭据重新Sign in一次并Retry。
     */
    suspend fun call(
        obj: String,
        method: String,
        params: JsonObject = JsonObject(emptyMap())
    ): UbusResult {
        val result = rawCall(sessionId, obj, method, params)
        if (result is UbusResult.UbusError && result.code == UbusErrorCode.PERMISSION_DENIED) {
            val u = username
            val p = password
            if (u != null && p != null) {
                val reLoginResult = login(u, p)
                if (reLoginResult is UbusResult.Success) {
                    return rawCall(sessionId, obj, method, params)
                }
                return reLoginResult
            }
        }
        return result
    }

    private suspend fun rawCall(
        session: String,
        obj: String,
        method: String,
        params: JsonObject
    ): UbusResult = withContext(Dispatchers.IO) {
        val requestBody = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("id", JsonPrimitive(requestIdCounter.getAndIncrement()))
            put("method", JsonPrimitive("call"))
            put("params", buildJsonArray {
                add(JsonPrimitive(session))
                add(JsonPrimitive(obj))
                add(JsonPrimitive(method))
                add(params)
            })
        }

        val request = Request.Builder()
            .url("$baseUrl/ubus")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UbusResult.NetworkError(
                        IOException("HTTP ${response.code}: ${response.message}")
                    )
                }
                val bodyStr = response.body?.string()
                    ?: return@withContext UbusResult.NetworkError(IOException("Empty response body"))
                parseRpcResponse(bodyStr)
            }
        } catch (e: IOException) {
            UbusResult.NetworkError(e)
        }
    }

    private fun parseRpcResponse(body: String): UbusResult {
        val root = Json.parseToJsonElement(body).jsonObject
        val error = root["error"]?.jsonObject
        if (error != null) {
            val code = error["code"]?.jsonPrimitive?.int ?: -1
            val message = error["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown JSON-RPC error"
            return UbusResult.UbusError(code, message)
        }

        val result = root["result"]
        // ubus 的 "result" 字段本身是一个数组：[ubus_status_code, {data...}]
        if (result is JsonArray) {
            val statusCode = result.getOrNull(0)?.jsonPrimitive?.int ?: -1
            val payload = result.getOrNull(1) ?: JsonObject(emptyMap())
            return if (statusCode == UbusErrorCode.OK) {
                UbusResult.Success(payload)
            } else {
                UbusResult.UbusError(statusCode, ubusErrorMessage(statusCode))
            }
        }

        return UbusResult.NetworkError(IOException("Unexpected RPC response shape: $body"))
    }

    private fun ubusErrorMessage(code: Int): String = when (code) {
        UbusErrorCode.INVALID_COMMAND -> "Invalid command"
        UbusErrorCode.INVALID_ARGUMENT -> "Invalid argument"
        UbusErrorCode.METHOD_NOT_FOUND -> "Method not found"
        UbusErrorCode.NOT_FOUND -> "Object not found"
        UbusErrorCode.NO_DATA -> "No data"
        UbusErrorCode.PERMISSION_DENIED -> "Permission denied"
        UbusErrorCode.TIMEOUT -> "Timeout"
        UbusErrorCode.NOT_SUPPORTED -> "Not supported"
        else -> "Unknown ubus error ($code)"
    }

    companion object {
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // opkg update/install 可能较慢
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

/** 一些从 JsonElement 里取值的小工具，供 Repository 层解析结果时用 */
fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.int
