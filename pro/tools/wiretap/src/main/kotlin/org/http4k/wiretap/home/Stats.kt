package org.http4k.wiretap.home

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
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.WiretapStats
import org.http4k.wiretap.util.Json
import java.time.Clock
import java.time.Duration

fun GetStats(
    clock: Clock,
    transactionStore: TransactionStore,
    traceStore: TraceStore,
    inboundChaos: ChaosEngine,
    outboundChaos: ChaosEngine
) = object : WiretapFunction {
    val startTime = clock.instant()

    private fun getStats(): WiretapStats {
        val now = clock.instant()
        return WiretapStats(
            uptime = formatUptime(Duration.between(startTime, now)),
            transactions = transactionStore.stats(startTime, now),
            traceCount = traceStore.traces().size,
            inboundChaosActive = inboundChaos.isEnabled(),
            inboundChaosDescription = inboundChaos.toString(),
            outboundChaosActive = outboundChaos.isEnabled(),
            outboundChaosDescription = outboundChaos.toString(),
        )
    }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = "/stats" bind GET to {
        Response(OK).datastarElements(
            elements(StatsView(getStats())),
            selector = Selector.of("#overview-stats")
        )
    }

    override fun mcp(): ToolCapability = Tool(
        "get_stats",
        "Get traffic overview including request counts, latency distribution, top hosts, and chaos engine status"
    ) bind {
        Json.asToolResponse(getStats())
    }
}

data class StatsView(val stats: WiretapStats) : ViewModel {
    val inboundChaosBadgeClass = if (stats.inboundChaosActive) "badge-chaos-active" else "badge-chaos-inactive"
    val inboundChaosBadgeText = if (stats.inboundChaosActive) "ACTIVE" else "INACTIVE"
    val outboundChaosBadgeClass = if (stats.outboundChaosActive) "badge-chaos-active" else "badge-chaos-inactive"
    val outboundChaosBadgeText = if (stats.outboundChaosActive) "ACTIVE" else "INACTIVE"
    val hasHosts = stats.transactions.topHosts.isNotEmpty()
    val statusJson = chartJson(
        listOf("2xx", "3xx", "4xx", "5xx"),
        listOf("2xx", "3xx", "4xx", "5xx").map { stats.transactions.statusCounts[it] ?: 0 })
    val methodJson =
        chartJson(stats.transactions.methodCounts.keys.toList(), stats.transactions.methodCounts.values.toList())
    val latencyJson = chartJson(
        listOf("0-10ms", "10-50ms", "50-100ms", "100-500ms", "500ms+"),
        listOf("0-10ms", "10-50ms", "50-100ms", "100-500ms", "500ms+").map {
            stats.transactions.latencyCounts[it] ?: 0
        })
    val trafficJson = chartJson(stats.transactions.trafficTimeline.first, stats.transactions.trafficTimeline.second)
}

private fun chartJson(labels: List<String>, data: List<Int>): String =
    Json.asFormatString(mapOf("labels" to labels, "data" to data))
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
