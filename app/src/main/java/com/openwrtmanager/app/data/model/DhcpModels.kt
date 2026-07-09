package com.openwrtmanager.app.data.model

data class DhcpPool(
    val sectionId: String,
    val interfaceName: String,   // "interface" option, 例如 "lan"
    val start: String?,
    val limit: String?,
    val leaseTime: String?,
    val ignore: Boolean          // 是否在该接口上禁用 DHCP
)

data class StaticLease(
    val sectionId: String,
    val name: String,
    val macAddress: String,
    val ipAddress: String,
    val leaseTime: String?
)
