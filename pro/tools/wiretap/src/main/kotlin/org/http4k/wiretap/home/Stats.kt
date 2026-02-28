package org.http4k.wiretap.home

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
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
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.HostBucket
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.WiretapStats
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.util.Json
import java.time.Clock
import java.time.Duration
import java.time.Instant

fun GetStats(
    clock: Clock,
    transactionStore: TransactionStore,
    traceStore: TraceStore,
    inboundChaos: ChaosEngine,
    outboundChaos: ChaosEngine
) = object : WiretapFunction {
    val startTime = clock.instant()

    private fun getStats(): WiretapStats {
        val txs = transactionStore.list()
        val now = clock.instant()
        val uptime = Duration.between(startTime, now)

        val inbound = txs.filter { it.direction == Inbound }
        val outbound = txs.filter { it.direction == Outbound }

        val statusCounts = txs.groupingBy { statusBucket(it.transaction.response.status.code) }.eachCount()
        val methodCounts = txs.groupingBy { it.transaction.request.method.name }.eachCount()
        val latencyCounts = txs.groupingBy { latencyBucket(it.transaction.duration) }.eachCount()

        val hosts = outbound
            .filter { it.transaction.request.uri.host.isNotEmpty() }
            .groupBy {
                val uri = it.transaction.request.uri
                if (uri.port != null) "${uri.host}:${uri.port}" else uri.host
            }
            .entries
            .sortedByDescending { it.value.size }
            .take(10)
            .map { (host, hostTxs) ->
                val codes = hostTxs.map { it.transaction.response.status.code }
                HostBucket(
                    host = host,
                    count = hostTxs.size,
                    avgLatencyMs = hostTxs.map { it.transaction.duration.toMillis() }.average().toLong(),
                    count2xx = codes.count { it in 200..299 },
                    count3xx = codes.count { it in 300..399 },
                    count4xx = codes.count { it in 400..499 },
                    count5xx = codes.count { it >= 500 }
                )
            }

        return WiretapStats(
            uptime = formatUptime(uptime),
            totalRequests = txs.size,
            inboundCount = inbound.size,
            outboundCount = outbound.size,
            traceCount = traceStore.traces().size,
            inboundChaosActive = inboundChaos.isEnabled(),
            inboundChaosDescription = inboundChaos.toString(),
            outboundChaosActive = outboundChaos.isEnabled(),
            outboundChaosDescription = outboundChaos.toString(),
            statusCounts = statusCounts,
            methodCounts = methodCounts,
            latencyCounts = latencyCounts,
            topHosts = hosts,
            trafficTimeline = trafficOverTime(txs, startTime, uptime, now)
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
        ToolResponse.Ok(listOf(Content.Text(Json.asFormatString(getStats()))))
    }
}

data class StatsView(val stats: WiretapStats) : ViewModel {
    val inboundChaosBadgeClass = if (stats.inboundChaosActive) "badge-chaos-active" else "badge-chaos-inactive"
    val inboundChaosBadgeText = if (stats.inboundChaosActive) "ACTIVE" else "INACTIVE"
    val outboundChaosBadgeClass = if (stats.outboundChaosActive) "badge-chaos-active" else "badge-chaos-inactive"
    val outboundChaosBadgeText = if (stats.outboundChaosActive) "ACTIVE" else "INACTIVE"
    val hasHosts = stats.topHosts.isNotEmpty()
    val statusJson = chartJson(
        listOf("2xx", "3xx", "4xx", "5xx"),
        listOf("2xx", "3xx", "4xx", "5xx").map { stats.statusCounts[it] ?: 0 })
    val methodJson = chartJson(stats.methodCounts.keys.toList(), stats.methodCounts.values.toList())
    val latencyJson = chartJson(
        listOf("0-10ms", "10-50ms", "50-100ms", "100-500ms", "500ms+"),
        listOf("0-10ms", "10-50ms", "50-100ms", "100-500ms", "500ms+").map { stats.latencyCounts[it] ?: 0 })
    val trafficJson = chartJson(stats.trafficTimeline.first, stats.trafficTimeline.second)
}

private fun chartJson(labels: List<String>, data: List<Int>): String =
    Json.asFormatString(mapOf("labels" to labels, "data" to data))
        .replace("\"", "&quot;")

private fun statusBucket(code: Int) = when {
    code >= 500 -> "5xx"
    code >= 400 -> "4xx"
    code >= 300 -> "3xx"
    else -> "2xx"
}

private fun latencyBucket(duration: Duration): String {
    val ms = duration.toMillis()
    return when {
        ms < 10 -> "0-10ms"
        ms < 50 -> "10-50ms"
        ms < 100 -> "50-100ms"
        ms < 500 -> "100-500ms"
        else -> "500ms+"
    }
}

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

private fun trafficOverTime(
    txs: List<WiretapTransaction>,
    startTime: Instant,
    uptime: Duration,
    now: Instant
): Pair<List<String>, List<Int>> {
    if (txs.isEmpty()) return Pair(emptyList(), emptyList())

    val bucketSeconds = when {
        uptime.toMinutes() < 5 -> 10L
        uptime.toHours() < 1 -> 60L
        uptime.toHours() < 6 -> 300L
        else -> 600L
    }

    val bucketCount = minOf((uptime.seconds / bucketSeconds + 1).toInt(), 60)
    val firstBucketStart = now.minusSeconds(bucketCount * bucketSeconds)

    val labels = (0 until bucketCount).map { i ->
        val bucketTime = firstBucketStart.plusSeconds((i + 1) * bucketSeconds)
        val ago = Duration.between(bucketTime, now)
        when {
            ago.seconds < 60 -> "${ago.seconds}s"
            ago.toMinutes() < 60 -> "${ago.toMinutes()}m"
            else -> "${ago.toHours()}h${ago.toMinutes() % 60}m"
        }
    }

    val counts = IntArray(bucketCount)
    txs.forEach { tx ->
        val txTime = tx.transaction.start
        if (txTime.isAfter(firstBucketStart)) {
            val idx = (Duration.between(firstBucketStart, txTime).seconds / bucketSeconds).toInt()
            if (idx in counts.indices) counts[idx]++
        }
    }

    return Pair(labels, counts.toList())
}
