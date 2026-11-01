package com.openwrtmanager.app.data.model

data class FirewallZone(
    val sectionId: String,
    val name: String,
    val input: String,     // ACCEPT / REJECT / DROP
    val output: String,
    val forward: String,
    val masq: Boolean,
    val networks: List<String>
)

data class FirewallRule(
    val sectionId: String,
    val name: String,
    val src: String?,          // 源Zones，例如 "wan"
    val dest: String?,         // 目的Zones，例如 "lan"
    val destPort: String?,
    val proto: String?,        // tcp/udp/tcp udp/icmp/...
    val target: String,        // ACCEPT/REJECT/DROP
    val enabled: Boolean
)
