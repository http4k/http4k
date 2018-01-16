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

    object Server {
        private val notAlphaNumUnderscore: Regex = "[^a-zA-Z0-9_]".toRegex()
        val defaultRequestIdFormatter: RequestIdFormatter = {
            X_URI_TEMPLATE(it)?.replace('/', '_')?.replace(notAlphaNumUnderscore, "") ?: "UNMAPPED"
        }

        object RequestTimer {
            operator fun invoke(meterRegistry: MeterRegistry,
                                name: String = "http.server.requests",
                                description: String? = "Timings of server requests",
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
        }

        object RequestCounter {
            operator fun invoke(meterRegistry: MeterRegistry,
                                name: String = "http.server.requests",
                                description: String? = "Total number of server requests",
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
    }

    object Client {
        val defaultRequestIdFormatter: RequestIdFormatter = { it.uri.host.replace('.', '_') }

        object RequestTimer {
            operator fun invoke(meterRegistry: MeterRegistry,
                                name: String = "http.client.request.latency",
                                description: String? = "Timing of client requests",
                                methodName: String = "method",
                                statusName: String = "status",
                                requestIdName: String = "host",
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
        }

        object RequestCounter {
            operator fun invoke(meterRegistry: MeterRegistry,
                                name: String = "http.client.request.count",
                                description: String? = "Total number of client requests",
                                methodName: String = "method",
                                statusName: String = "status",
                                requestIdName: String = "host",
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
    }
}
