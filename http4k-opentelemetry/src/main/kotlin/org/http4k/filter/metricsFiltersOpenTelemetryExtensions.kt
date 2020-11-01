package org.http4k.filter

import io.opentelemetry.common.Labels
import io.opentelemetry.metrics.Meter
import org.http4k.core.Filter
import org.http4k.core.HttpTransaction
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import java.time.Clock

object MetricsFilters {
    open class MetricsFilterTemplate(private val defaultTimer: Pair<String, String>,
                                     private val defaultCounter: Pair<String, String>,
                                     private val defaultLabeler: HttpTransactionLabeler) {
        fun RequestTimer(meter: Meter,
                         name: String = defaultTimer.first,
                         description: String? = defaultTimer.second,
                         labeler: HttpTransactionLabeler = defaultLabeler,
                         clock: Clock = Clock.systemUTC()): Filter {
            val meterInstance = meter.longValueRecorderBuilder(name).setDescription(description).build()

            return ReportHttpTransaction(clock) { tx ->
                meterInstance.record(tx.duration.toMillis(), Labels.of(labeler.labels(tx)))
            }
        }

        fun RequestCounter(meter: Meter,
                           name: String = defaultCounter.first,
                           description: String? = defaultCounter.second,
                           labeler: HttpTransactionLabeler = defaultLabeler,
                           clock: Clock = Clock.systemUTC()): Filter {
            val meterInstance = meter.longCounterBuilder(name).setDescription(description).build()

            return ReportHttpTransaction(clock) {
                meterInstance.add(1, Labels.of(labeler.labels(it)))
            }
        }

        private fun HttpTransactionLabeler.labels(tx: HttpTransaction) =
            this(tx).labels.map { listOf(it.key, it.value) }.flatten().toTypedArray()
    }

    private val notAlphaNumUnderscore: Regex = "[^a-zA-Z0-9_]".toRegex()

    object Server : MetricsFilterTemplate(
        "http.server.request.latency" to "Timing of server requests",
        "http.server.request.count" to "Total number of server requests",
        {
            it.copy(labels = mapOf(
                "method" to it.request.method.toString(),
                "status" to it.response.status.code.toString(),
                "path" to it.routingGroup.replace('/', '_').replace(notAlphaNumUnderscore, "")
            ))
        }
    )

    object Client : MetricsFilterTemplate(
        "http.client.request.latency" to "Timing of client requests",
        "http.client.request.count" to "Total number of client requests",
        {
            it.copy(labels = mapOf(
                "method" to it.request.method.toString(),
                "status" to it.response.status.code.toString(),
                "host" to it.request.uri.host.replace('.', '_')
            ))
        }
    )
}
