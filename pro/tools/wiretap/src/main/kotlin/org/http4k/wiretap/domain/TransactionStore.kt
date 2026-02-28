package org.http4k.wiretap.domain

import org.http4k.core.HttpTransaction
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

interface TransactionStore {
    fun record(transaction: HttpTransaction, direction: Direction): WiretapTransaction
    fun list(
        filter: TransactionFilter = TransactionFilter(),
        limit: Int = 500,
        cursor: Long? = null
    ): List<WiretapTransaction>

    fun get(id: Long): WiretapTransaction?
    fun stats(startTime: Instant, now: Instant): TransactionStats
    fun subscribe(fn: (WiretapTransaction) -> Unit): () -> Unit
    fun clear()

    companion object {
        fun InMemory(maxSize: Int = 500) = object : TransactionStore {
            private val nextId = AtomicLong()
            private val transactions = ConcurrentLinkedDeque<WiretapTransaction>()
            private val subscribers = CopyOnWriteArrayList<(WiretapTransaction) -> Unit>()

            override fun record(transaction: HttpTransaction, direction: Direction): WiretapTransaction {
                val wiretapTransaction = WiretapTransaction(nextId.incrementAndGet(), transaction, direction)
                transactions.addFirst(wiretapTransaction)
                while (transactions.size > maxSize) {
                    transactions.removeLast()
                }
                subscribers.forEach { it(wiretapTransaction) }
                return wiretapTransaction
            }

            override fun list(filter: TransactionFilter, limit: Int, cursor: Long?): List<WiretapTransaction> {
                val base = if (cursor != null) transactions.filter { it.id < cursor } else transactions.toList()
                return base
                    .filter { it.toSummary().matches(filter) }
                    .take(limit)
            }

            override fun get(id: Long): WiretapTransaction? = transactions.find { it.id == id }

            override fun stats(startTime: Instant, now: Instant): TransactionStats {
                val txs = transactions.toList()
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

                return TransactionStats(
                    totalRequests = txs.size,
                    inboundCount = inbound.size,
                    outboundCount = outbound.size,
                    statusCounts = statusCounts,
                    methodCounts = methodCounts,
                    latencyCounts = latencyCounts,
                    topHosts = hosts,
                    trafficTimeline = trafficOverTime(txs, uptime, now)
                )
            }

            override fun subscribe(fn: (WiretapTransaction) -> Unit): () -> Unit {
                subscribers.add(fn)
                return { subscribers.remove(fn) }
            }

            override fun clear() {
                transactions.clear()
            }
        }
    }
}

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

private fun trafficOverTime(
    txs: List<WiretapTransaction>,
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
