package org.http4k.filter

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.lens.Header.X_URI_TEMPLATE
import java.time.Clock

typealias RequestIdFormatter = (Request) -> String

object MetricFilters {

    open class FiltersTemplate(private val defaultTimer: Pair<String, String>,
                               private val defaultCounter: Pair<String, String>,
                               private val defaultRequestIdName: String,
                               internal val defaultRequestIdFormatter: RequestIdFormatter) {
        fun RequestTimer(meterRegistry: MeterRegistry,
                         name: String = defaultTimer.first,
                         description: String? = defaultTimer.second,
                         methodName: String = "method",
                         statusName: String = "status",
                         requestIdName: String = defaultRequestIdName,
                         requestIdFormatter: RequestIdFormatter = defaultRequestIdFormatter,
                         clock: Clock = Clock.systemUTC()): Filter =
            ResponseFilters.ReportHttpTransaction(clock) { tx, _ ->
                Timer.builder(name).description(description)
                    .tag(methodName, tx.request.method.name)
                    .tag(statusName, tx.response.status.code.toString())
                    .tag(requestIdName, requestIdFormatter(tx.request))
                    .register(meterRegistry)
                    .record(tx.duration)
            }

        fun RequestCounter(meterRegistry: MeterRegistry,
                           name: String = defaultCounter.first,
                           description: String? = defaultCounter.second,
                           methodName: String = "method",
                           statusName: String = "status",
                           requestIdName: String = defaultRequestIdName,
                           requestIdFormatter: RequestIdFormatter = defaultRequestIdFormatter): Filter =
            Filter { next ->
                { request ->
                    next(request).apply {
                        Counter.builder(name).description(description)
                            .tag(methodName, request.method.name)
                            .tag(statusName, status.code.toString())
                            .tag(requestIdName, requestIdFormatter(request))
                            .register(meterRegistry)
                            .increment()
                    }
                }
            }
    }

    private val notAlphaNumUnderscore: Regex = "[^a-zA-Z0-9_]".toRegex()

    object Server : FiltersTemplate(
        "http.server.request.latency" to "Timing of server requests",
        "http.server.request.count" to "Total number of server requests",
        "path",
        {
            X_URI_TEMPLATE(it)?.replace('/', '_')?.replace(notAlphaNumUnderscore, "") ?: "UNMAPPED"
        }
    )

    object Client : FiltersTemplate(
        "http.client.request.latency" to "Timing of client requests",
        "http.client.request.count" to "Total number of client requests",
        "host",
        { it.uri.host.replace('.', '_') }
    )
}
