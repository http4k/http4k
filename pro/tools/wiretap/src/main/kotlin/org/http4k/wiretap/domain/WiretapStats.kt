package org.http4k.wiretap.domain

data class JvmMetrics(
    val heapUsedMb: Long,
    val heapMaxMb: Long,
    val heapCommittedMb: Long,
    val nonHeapUsedMb: Long,
    val threadCount: Int,
    val daemonThreadCount: Int,
    val peakThreadCount: Int,
    val gcPauseCount: Long,
    val gcPauseTotalMs: Long,
    val cpuUsage: Double,
    val systemCpuUsage: Double,
    val classesLoaded: Int,
    val classesUnloaded: Long
)

data class WiretapStats(
    val uptime: String,
    val totalRequests: Long,
    val inboundCount: Long,
    val outboundCount: Long,
    val latencyCounts: Map<String, Int>,
    val trafficTimeline: TrafficTimeline,
    val hostTimelines: Map<String, TrafficTimeline>,
    val traceCount: Int,
    val inboundChaosActive: Boolean,
    val inboundChaosDescription: String,
    val outboundChaosActive: Boolean,
    val outboundChaosDescription: String,
    val jvm: JvmMetrics
)

