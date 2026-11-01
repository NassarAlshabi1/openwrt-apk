package com.openwrtmanager.app.data.repository

import com.openwrtmanager.app.data.model.FirewallRule
import com.openwrtmanager.app.data.model.FirewallZone
import com.openwrtmanager.app.data.model.UciSection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firewall管理：本质是对 "firewall" 这个 UCI config 的语义化封装。
 * Zone 对应 type == "zone" 的 section，Rule 对应 type == "rule" 的 section。
 */
@Singleton
class FirewallRepository @Inject constructor(
    private val uciRepository: UciRepository
) {
    suspend fun getZones(): Result<List<FirewallZone>> =
        uciRepository.getConfig("firewall").map { config ->
            config.sections.filter { it.type == "zone" }.map { it.toZone() }
        }

    suspend fun getRules(): Result<List<FirewallRule>> =
        uciRepository.getConfig("firewall").map { config ->
            config.sections.filter { it.type == "rule" }.map { it.toRule() }
        }

    suspend fun setRuleEnabled(sectionId: String, enabled: Boolean): Result<Unit> =
        uciRepository.setValues("firewall", sectionId, mapOf("enabled" to if (enabled) "1" else "0"))

    suspend fun deleteRule(sectionId: String): Result<Unit> =
        uciRepository.deleteSection("firewall", sectionId)

    suspend fun addRule(
        name: String,
        src: String?,
        dest: String?,
        destPort: String?,
        proto: String?,
        target: String
    ): Result<String> = uciRepository.addSection(
        configName = "firewall",
        type = "rule",
        values = buildMap {
            put("name", name)
            src?.let { put("src", it) }
            dest?.let { put("dest", it) }
            destPort?.let { put("dest_port", it) }
            proto?.let { put("proto", it) }
            put("target", target)
            put("enabled", "1")
        }
    )

    /** 提交并使Firewall重新加载生效 */
    suspend fun commitAndApply(): Result<Unit> {
        val commitResult = uciRepository.commit("firewall")
        if (commitResult.isFailure) return commitResult
        return uciRepository.applyChanges("firewall")
    }

    suspend fun discardChanges(): Result<Unit> = uciRepository.revert("firewall")

    private fun UciSection.toZone(): FirewallZone = FirewallZone(
        sectionId = sectionId,
        name = option("name") ?: sectionId,
        input = option("input") ?: "DROP",
        output = option("output") ?: "ACCEPT",
        forward = option("forward") ?: "DROP",
        masq = option("masq") == "1",
        networks = listOptions["network"] ?: (option("network")?.let { listOf(it) } ?: emptyList())
    )

    private fun UciSection.toRule(): FirewallRule = FirewallRule(
        sectionId = sectionId,
        name = option("name") ?: "(未命名Rules)",
        src = option("src"),
        dest = option("dest"),
        destPort = option("dest_port"),
        proto = option("proto"),
        target = option("target") ?: "ACCEPT",
        enabled = option("enabled") != "0"
    )
}
