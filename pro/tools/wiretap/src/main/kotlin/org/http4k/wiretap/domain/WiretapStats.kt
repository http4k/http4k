package org.http4k.wiretap.domain

data class WiretapStats(
    val uptime: String,
    val totalRequests: Int,
    val inboundCount: Int,
    val outboundCount: Int,
    val traceCount: Int,
    val inboundChaosActive: Boolean,
    val inboundChaosDescription: String,
    val outboundChaosActive: Boolean,
    val outboundChaosDescription: String,
    val statusCounts: Map<String, Int>,
    val methodCounts: Map<String, Int>,
    val latencyCounts: Map<String, Int>,
    val topHosts: List<HostBucket>,
    val trafficTimeline: Pair<List<String>, List<Int>>
)

data class TransactionStats(
    val totalRequests: Int,
    val inboundCount: Int,
    val outboundCount: Int,
    val statusCounts: Map<String, Int>,
    val methodCounts: Map<String, Int>,
    val latencyCounts: Map<String, Int>,
    val topHosts: List<HostBucket>,
    val trafficTimeline: Pair<List<String>, List<Int>>
)

data class HostBucket(
    val host: String,
    val count: Int,
    val avgLatencyMs: Long,
    val count2xx: Int,
    val count3xx: Int,
    val count4xx: Int,
    val count5xx: Int,
)
