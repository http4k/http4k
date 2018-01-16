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

    private val notAlphaNumUnderscore: Regex = "[^a-zA-Z0-9_]".toRegex()

    abstract class Filters(
        private val defaultTimer: Pair<String, String>,
        private val defaultCounter: Pair<String, String>,
        internal val defaultRequestIdFormatter: RequestIdFormatter) {

        /**
         * Time latency of all requests for this route.
         */
        fun RequestTimer(meterRegistry: MeterRegistry,
                         name: String = defaultTimer.first,
                         description: String? = defaultTimer.second,
                         methodName: String = "method",
                         statusName: String = "status",
                         requestIdName: String = "path",
                         requestIdFormatter: RequestIdFormatter = defaultRequestIdFormatter,
                         clock: Clock = Clock.systemUTC()): Filter =
            ResponseFilters.ReportLatency(clock) { request, response, duration ->
                Timer.builder(name).description(description)
                    .tag(methodName, request.method.name)
                    .tag(statusName, response.status.code.toString())
                    .tag(requestIdName, requestIdFormatter(request))
                    .register(meterRegistry)
                    .record(duration)
            }

        /**
         * Count all requests for this route.
         */
        fun RequestCounter(meterRegistry: MeterRegistry,
                           name: String = defaultCounter.first,
                           description: String? = defaultCounter.second,
                           methodName: String = "method",
                           statusName: String = "status",
                           requestIdName: String = "path",
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

    object Server : Filters(
        "http.server.request.latency" to "Timing of server requests",
        "http.server.request.count" to "Total number of server requests",
        {
            X_URI_TEMPLATE(it)?.replace('/', '_')?.replace(notAlphaNumUnderscore, "") ?: "UNMAPPED"
        })

    object Client : Filters(
        "http.client.request.latency" to "Timing of client requests",
        "http.client.request.count" to "Total number of client requests",
        { it.uri.host.replace('.', '_') })
}
