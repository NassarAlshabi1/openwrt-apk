package com.openwrtmanager.app.data.model

/**
 * UCI 配置的通用表示。一个 config（例如 "firewall"）由若干 section 组成，
 * 每个 section 有类型（".type"，如 "rule" / "zone" / "host"）和一组 key-value。
 *
 * ubus 的 uci.get 返回结构大致是：
 * {
 *   "values": {
 *     "cfg0a1b2c": { ".type": "zone", ".name": "cfg0a1b2c", "name": "lan", ... },
 *     ...
 *   }
 * }
 *
 * 这里用 Map<String, String> 承载 options 的值（UCI 的值本质都是字符串或字符串列表，
 * 列表用 List<String> 单独承载于 listOptions）。
 */
data class UciSection(
    val sectionId: String,               // ".name"，例如 "cfg0a1b2c" 或用户自定义的具名 section
    val type: String,                    // ".type"，例如 "rule", "zone", "host", "dnsmasq"
    val options: Map<String, String> = emptyMap(),
    val listOptions: Map<String, List<String>> = emptyMap()
) {
    fun option(key: String): String? = options[key]
}

data class UciConfig(
    val configName: String,              // 例如 "firewall", "dhcp", "network"
    val sections: List<UciSection>
)
