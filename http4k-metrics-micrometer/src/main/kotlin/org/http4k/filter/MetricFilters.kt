package org.http4k.filter

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.http4k.core.Filter
import java.time.Clock

object MetricFilters {

    open class FiltersTemplate(
        private val defaultTimer: Pair<String, String>,
        private val defaultCounter: Pair<String, String>,
        private val defaultLabeller: HttpTransactionLabeller
    ) {
        fun RequestTimer(
            meterRegistry: MeterRegistry,
            name: String = defaultTimer.first,
            description: String? = defaultTimer.second,
            labeller: HttpTransactionLabeller = defaultLabeller,
            clock: Clock = Clock.systemUTC()
        ): Filter =
            ResponseFilters.ReportHttpTransaction(clock) { tx ->
                labeller(tx).labels.entries.fold(Timer.builder(name).description(description)) { memo, next ->
                    memo.tag(next.key, next.value)
                }.register(meterRegistry).record(tx.duration)
            }

        fun RequestCounter(
            meterRegistry: MeterRegistry,
            name: String = defaultCounter.first,
            description: String? = defaultCounter.second,
            labeller: HttpTransactionLabeller = defaultLabeller
        ): Filter =
            ResponseFilters.ReportHttpTransaction(Clock.systemUTC()) { tx ->
                labeller(tx).labels.entries.fold(Counter.builder(name).description(description)) { memo, next ->
                    memo.tag(next.key, next.value)
                }.register(meterRegistry).increment()
            }
    }

    private val notAlphaNumUnderscore: Regex = "[^a-zA-Z0-9_]".toRegex()

    object Server : FiltersTemplate(
        "http.server.request.latency" to "Timing of server requests",
        "http.server.request.count" to "Total number of server requests",
        {
            it.copy(
                labels = mapOf(
                    "method" to it.request.method.toString(),
                    "status" to it.response.status.code.toString(),
                    "path" to it.routingGroup.replace('/', '_').replace(notAlphaNumUnderscore, "")
                )
            )
        }
    )

    object Client : FiltersTemplate(
        "http.client.request.latency" to "Timing of client requests",
        "http.client.request.count" to "Total number of client requests",
        {
            it.copy(
                labels = mapOf(
                    "method" to it.request.method.toString(),
                    "status" to it.response.status.code.toString(),
                    "host" to it.request.uri.host.replace('.', '_')
                )
            )
        }
    )
}
