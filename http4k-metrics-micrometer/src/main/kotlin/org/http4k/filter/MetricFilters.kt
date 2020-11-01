package org.http4k.filter

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.http4k.core.Filter
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import java.time.Clock

object MetricFilters {

    open class MetricsFilterTemplate(private val defaultTimer: Pair<String, String>,
                                     private val defaultCounter: Pair<String, String>,
                                     private val defaultLabeler: HttpTransactionLabeller) {
        fun RequestTimer(meterRegistry: MeterRegistry,
                         name: String = defaultTimer.first,
                         description: String? = defaultTimer.second,
                         labeller: HttpTransactionLabeller = defaultLabeler,
                         clock: Clock = Clock.systemUTC()): Filter =
            ReportHttpTransaction(clock) {
                labeller(it).labels.entries.fold(Timer.builder(name).description(description)) { memo, next ->
                    memo.tag(next.key, next.value)
                }.register(meterRegistry).record(it.duration)
            }

        fun RequestCounter(meterRegistry: MeterRegistry,
                           name: String = defaultCounter.first,
                           description: String? = defaultCounter.second,
                           labeler: HttpTransactionLabeller = defaultLabeler,
                           clock: Clock = Clock.systemUTC()): Filter =
            ReportHttpTransaction(clock) {
                labeler(it).labels.entries.fold(Counter.builder(name).description(description)) { memo, next ->
                    memo.tag(next.key, next.value)
                }.register(meterRegistry).increment()
            }
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
