package com.openwrtmanager.app.data.remote

import kotlinx.serialization.json.JsonElement

/**
 * ubus JSON-RPC 调用的标准错误码（部分常用）：
 * 0  = OK
 * 1  = Invalid command
 * 2  = Invalid argument
 * 3  = Method not found
 * 4  = Not found
 * 5  = No data
 * 6  = Permission denied（常见于 session 过期或 ACL 不允许）
 * 7  = Timeout
 * 9  = Not supported
 */
object UbusErrorCode {
    const val OK = 0
    const val INVALID_COMMAND = 1
    const val INVALID_ARGUMENT = 2
    const val METHOD_NOT_FOUND = 3
    const val NOT_FOUND = 4
    const val NO_DATA = 5
    const val PERMISSION_DENIED = 6
    const val TIMEOUT = 7
    const val NOT_SUPPORTED = 9
}

sealed class UbusResult {
    data class Success(val data: JsonElement) : UbusResult()
    data class UbusError(val code: Int, val message: String) : UbusResult()
    data class NetworkError(val throwable: Throwable) : UbusResult()
}
