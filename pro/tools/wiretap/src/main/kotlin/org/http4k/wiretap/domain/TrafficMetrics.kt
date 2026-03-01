package org.http4k.wiretap.domain

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.http4k.core.HttpTransaction
import org.http4k.core.Uri
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class TrafficTimeline(
    val labels: List<String>,
    val datasets: Map<String, List<Int>>
)

class TrafficMetrics(
    private val meterRegistry: MeterRegistry,
    private val bucketSeconds: Long = 10L,
    private val maxBuckets: Int = 60,
    private val clock: Clock = Clock.systemUTC()
) {
    private val statusBuckets = listOf("2xx", "3xx", "4xx", "5xx")

    private data class Snapshot(
        val time: Instant,
        val totals: Map<String, Long>,
        val hostTotals: Map<String, Map<String, Long>>
    )

    private val snapshots = ArrayDeque<Snapshot>()

    private val hostCounters = mutableMapOf<String, MutableMap<String, Counter>>()

    fun record(transaction: HttpTransaction, direction: Direction) {
        val bucket = statusBucket(transaction.response.status.code)
        counter("wiretap.traffic", "status", bucket, "direction", direction.name).increment()
        counter("wiretap.traffic.total", "direction", direction.name).increment()

        val latency = latencyBucket(transaction.duration)
        counter("wiretap.latency", "bucket", latency).increment()

        if (direction == Direction.Outbound) {
            val host = hostKey(transaction.request.uri)
            if (host.isNotEmpty()) {
                hostCounter(host, bucket).increment()
                counter("wiretap.host.latency", "host", host).increment()
                meterRegistry.counter("wiretap.host.latency.total", "host", host)
                    .increment(transaction.duration.toMillis().toDouble())
            }
        }
    }

    fun snapshot() {
        val now = clock.instant()
        val totals = statusBuckets.associateWith { bucket ->
            counter("wiretap.traffic", "status", bucket, "direction", Direction.Inbound.name).count().toLong() +
                counter("wiretap.traffic", "status", bucket, "direction", Direction.Outbound.name).count().toLong()
        }

        val hostTotals = hostCounters.map { (host, counters) ->
            host to statusBuckets.associateWith { bucket ->
                counters[bucket]?.count()?.toLong() ?: 0L
            }
        }.toMap()

        snapshots.addLast(Snapshot(now, totals, hostTotals))
        while (snapshots.size > maxBuckets + 1) {
            snapshots.removeFirst()
        }
    }

    fun trafficTimeline(): TrafficTimeline = buildTimeline(snapshots.map { it.time to it.totals })

    private fun buildTimeline(snapshots: List<Pair<Instant, Map<String, Long>>>): TrafficTimeline {
        if (snapshots.isEmpty()) {
            return TrafficTimeline(labels = emptyList(), datasets = statusBuckets.associateWith { emptyList() })
        }

        if (snapshots.size == 1) {
            val (time, totals) = snapshots.first()
            return TrafficTimeline(
                labels = listOf(labelFor(time)),
                datasets = statusBuckets.associateWith { listOf(totals[it]?.toInt() ?: 0) }
            )
        }

        val deltas = snapshots.zipWithNext().map { (prev, curr) ->
            curr.first to statusBuckets.associateWith { bucket ->
                ((curr.second[bucket] ?: 0) - (prev.second[bucket] ?: 0)).toInt()
            }
        }

        return TrafficTimeline(
            labels = deltas.map { (time, _) -> labelFor(time) },
            datasets = statusBuckets.associateWith { bucket -> deltas.map { (_, d) -> d[bucket] ?: 0 } }
        )
    }

    fun hostTimelines(): Map<String, TrafficTimeline> {
        if (snapshots.isEmpty()) return emptyMap()

        val allHosts = snapshots.flatMap { it.hostTotals.keys }.toSet()

        return allHosts.associateWith { host ->
            buildTimeline(snapshots.map { snap ->
                snap.time to statusBuckets.associateWith { bucket ->
                    snap.hostTotals[host]?.get(bucket) ?: 0L
                }
            })
        }
    }

    fun latencyCounts(): Map<String, Int> {
        val buckets = listOf("0-10ms", "10-50ms", "50-100ms", "100-500ms", "500ms+")
        return buckets.associateWith { counter("wiretap.latency", "bucket", it).count().toInt() }
    }

    fun topHosts(limit: Int = 10): List<HostBucket> =
        hostCounters.keys.map { host ->
            val statusCounts = statusBuckets.associateWith { bucket ->
                hostCounters[host]?.get(bucket)?.count()?.toInt() ?: 0
            }
            val totalCount = statusCounts.values.sum()
            val totalLatencyMs = meterRegistry.counter("wiretap.host.latency.total", "host", host).count().toLong()
            val avgLatency = if (totalCount > 0) totalLatencyMs / totalCount else 0L

            HostBucket(
                host = host,
                count = totalCount,
                avgLatencyMs = avgLatency,
                count2xx = statusCounts["2xx"] ?: 0,
                count3xx = statusCounts["3xx"] ?: 0,
                count4xx = statusCounts["4xx"] ?: 0,
                count5xx = statusCounts["5xx"] ?: 0
            )
        }.sortedByDescending { it.count }.take(limit)

    fun totalRequests(): Long =
        counter("wiretap.traffic.total", "direction", Direction.Inbound.name).count().toLong() +
            counter("wiretap.traffic.total", "direction", Direction.Outbound.name).count().toLong()

    fun inboundCount(): Long =
        counter("wiretap.traffic.total", "direction", Direction.Inbound.name).count().toLong()

    fun outboundCount(): Long =
        counter("wiretap.traffic.total", "direction", Direction.Outbound.name).count().toLong()

    private fun counter(name: String, vararg tags: String): Counter =
        meterRegistry.counter(name, *tags)

    private fun hostCounter(host: String, bucket: String): Counter {
        val counters = hostCounters.getOrPut(host) { mutableMapOf() }
        return counters.getOrPut(bucket) {
            meterRegistry.counter("wiretap.traffic.host", "host", host, "status", bucket)
        }
    }

    private fun labelFor(time: Instant): String {
        val ago = Duration.between(time, clock.instant())
        return when {
            ago.seconds < 60 -> "${ago.seconds}s"
            ago.toMinutes() < 60 -> "${ago.toMinutes()}m"
            else -> "${ago.toHours()}h${ago.toMinutes() % 60}m"
        }
    }

    private fun hostKey(uri: Uri): String {
        val host = uri.host
        if (host.isEmpty()) return ""
        return if (uri.port != null) "$host:${uri.port}" else host
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

    private fun statusBucket(code: Int) = when {
        code >= 500 -> "5xx"
        code >= 400 -> "4xx"
        code >= 300 -> "3xx"
        else -> "2xx"
    }
}
