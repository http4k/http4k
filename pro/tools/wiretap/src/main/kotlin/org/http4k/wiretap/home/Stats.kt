package org.http4k.wiretap.home

import io.micrometer.core.instrument.MeterRegistry
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.JvmMetrics
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TrafficMetrics
import org.http4k.wiretap.domain.TrafficTimeline
import org.http4k.wiretap.domain.WiretapStats
import org.http4k.wiretap.util.Json
import java.time.Duration
import java.util.concurrent.TimeUnit

fun GetStats(
    trafficMetrics: TrafficMetrics,
    traceStore: TraceStore,
    inboundChaos: ChaosEngine,
    outboundChaos: ChaosEngine,
    mcpCapabilities: McpCapabilities,
    meterRegistry: MeterRegistry
) = object : WiretapFunction {

    private fun gauge(name: String, vararg tags: String): Double =
        meterRegistry.find(name).tags(*tags).gauge()?.value() ?: 0.0

    private fun getStats(): WiretapStats {
        val uptimeSeconds = gauge("process.uptime").toLong()
        return WiretapStats(
            uptime = formatUptime(Duration.ofSeconds(uptimeSeconds)),
            totalRequests = trafficMetrics.totalRequests(),
            inboundCount = trafficMetrics.inboundCount(),
            outboundCount = trafficMetrics.outboundCount(),
            latencyCounts = trafficMetrics.latencyCounts(),
            trafficTimeline = trafficMetrics.trafficTimeline(),
            hostTimelines = trafficMetrics.hostTimelines(),
            traceCount = traceStore.traces().size,
            inboundChaosActive = inboundChaos.isEnabled(),
            inboundChaosDescription = inboundChaos.toString(),
            outboundChaosActive = outboundChaos.isEnabled(),
            outboundChaosDescription = outboundChaos.toString(),
            jvm = jvmMetrics()
        )
    }

    private fun jvmMetrics(): JvmMetrics {
        val gcTimer = meterRegistry.find("jvm.gc.pause").timer()

        return JvmMetrics(
            heapUsedMb = (gauge("jvm.memory.used", "area", "heap") / 1_048_576).toLong(),
            heapMaxMb = (gauge("jvm.memory.max", "area", "heap") / 1_048_576).toLong(),
            heapCommittedMb = (gauge("jvm.memory.committed", "area", "heap") / 1_048_576).toLong(),
            nonHeapUsedMb = (gauge("jvm.memory.used", "area", "nonheap") / 1_048_576).toLong(),
            threadCount = gauge("jvm.threads.live").toInt(),
            daemonThreadCount = gauge("jvm.threads.daemon").toInt(),
            peakThreadCount = gauge("jvm.threads.peak").toInt(),
            gcPauseCount = gcTimer?.count() ?: 0,
            gcPauseTotalMs = gcTimer?.totalTime(TimeUnit.MILLISECONDS)?.toLong() ?: 0,
            cpuUsage = gauge("process.cpu.usage"),
            systemCpuUsage = gauge("system.cpu.usage"),
            classesLoaded = gauge("jvm.classes.loaded").toInt(),
            classesUnloaded = gauge("jvm.classes.unloaded").toLong()
        )
    }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = "/stats" bind GET to {
        Response(OK).datastarElements(
            elements(StatsView(getStats(), mcpCapabilities)),
            selector = Selector.of("#overview-stats")
        )
    }

    override fun mcp(): ToolCapability = Tool(
        "get_stats",
        "Get traffic overview including request counts, latency distribution, and chaos engine status"
    ) bind {
        Json.asToolResponse(getStats())
    }
}

data class McpCapabilities(
    val security: String,
    val toolCount: Int = 0,
    val promptCount: Int = 0
) {
    val mcpUrl = "/_wiretap/mcp"
}

data class StatsView(val stats: WiretapStats, val mcp: McpCapabilities) : ViewModel {
    val inboundChaosBadgeClass = if (stats.inboundChaosActive) "badge-chaos-active" else "badge-chaos-inactive"
    val inboundChaosBadgeText = if (stats.inboundChaosActive) "ACTIVE" else "INACTIVE"
    val outboundChaosBadgeClass = if (stats.outboundChaosActive) "badge-chaos-active" else "badge-chaos-inactive"
    val outboundChaosBadgeText = if (stats.outboundChaosActive) "ACTIVE" else "INACTIVE"
    val heapPercent = if (stats.jvm.heapMaxMb > 0) (stats.jvm.heapUsedMb * 100 / stats.jvm.heapMaxMb).toInt() else 0
    val cpuPercent = "%.1f%%".format(stats.jvm.cpuUsage * 100)
    val systemCpuPercent = "%.1f%%".format(stats.jvm.systemCpuUsage * 100)
    val gcPauseTotalFormatted =
        if (stats.jvm.gcPauseTotalMs < 1000) "${stats.jvm.gcPauseTotalMs}ms" else "%.1fs".format(stats.jvm.gcPauseTotalMs / 1000.0)
    val latencyJson = chartJson(
        listOf("0-10ms", "10-50ms", "50-100ms", "100-500ms", "500ms+"),
        listOf("0-10ms", "10-50ms", "50-100ms", "100-500ms", "500ms+").map {
            stats.latencyCounts[it] ?: 0
        })
    val trafficJson = stackedChartJson(stats.trafficTimeline)
    val hostTimelinesJson = stats.hostTimelines.map { (host, timeline) ->
        HostTimelineEntry(host, stackedChartJson(timeline))
    }
    val hasHostTimelines = hostTimelinesJson.isNotEmpty()
}

data class HostTimelineEntry(val host: String, val json: String)

private fun chartJson(labels: List<String>, data: List<Int>): String =
    Json.asFormatString(mapOf("labels" to labels, "data" to data))
        .replace("\"", "&quot;")

private fun stackedChartJson(timeline: TrafficTimeline): String =
    Json.asFormatString(mapOf("labels" to timeline.labels, "datasets" to timeline.datasets))
        .replace("\"", "&quot;")

private fun formatUptime(duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
