package com.openwrtmanager.app.data.repository

import com.openwrtmanager.app.data.model.DhcpPool
import com.openwrtmanager.app.data.model.StaticLease
import com.openwrtmanager.app.data.model.UciSection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DHCP 管理：对应 "dhcp" 这个 UCI config。
 * type == "dhcp" 的 section 是每个接口的 DHCP 池配置；
 * type == "host" 的 section 是Static leases（MAC-IP 绑定）。
 */
@Singleton
class DhcpRepository @Inject constructor(
    private val uciRepository: UciRepository
) {
    suspend fun getPools(): Result<List<DhcpPool>> =
        uciRepository.getConfig("dhcp").map { config ->
            config.sections.filter { it.type == "dhcp" }.map { it.toPool() }
        }

    suspend fun getStaticLeases(): Result<List<StaticLease>> =
        uciRepository.getConfig("dhcp").map { config ->
            config.sections.filter { it.type == "host" }.map { it.toLease() }
        }

    suspend fun addStaticLease(name: String, mac: String, ip: String): Result<String> =
        uciRepository.addSection(
            configName = "dhcp",
            type = "host",
            values = mapOf(
                "name" to name,
                "mac" to mac,
                "ip" to ip
            )
        )

    suspend fun deleteStaticLease(sectionId: String): Result<Unit> =
        uciRepository.deleteSection("dhcp", sectionId)

    suspend fun setPoolIgnored(sectionId: String, ignore: Boolean): Result<Unit> =
        uciRepository.setValues("dhcp", sectionId, mapOf("ignore" to if (ignore) "1" else "0"))

    /** 提交并让 dnsmasq/odhcpd 重新加载生效 */
    suspend fun commitAndApply(): Result<Unit> {
        val commitResult = uciRepository.commit("dhcp")
        if (commitResult.isFailure) return commitResult
        return uciRepository.applyChanges("dnsmasq")
    }

    suspend fun discardChanges(): Result<Unit> = uciRepository.revert("dhcp")

    private fun UciSection.toPool(): DhcpPool = DhcpPool(
        sectionId = sectionId,
        interfaceName = option("interface") ?: sectionId,
        start = option("start"),
        limit = option("limit"),
        leaseTime = option("leasetime"),
        ignore = option("ignore") == "1"
    )

    private fun UciSection.toLease(): StaticLease = StaticLease(
        sectionId = sectionId,
        name = option("name") ?: "(未命名)",
        macAddress = option("mac") ?: "-",
        ipAddress = option("ip") ?: "-",
        leaseTime = option("leasetime")
    )
}
