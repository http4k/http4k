package org.http4k.filter

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.Header
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.http4k.routing.RoutingHttpHandler
import java.time.Duration

object MetricFilters {
    object Server {
        operator fun invoke(meterRegistry: MeterRegistry, config: Config = Config()): Filter {

            val clock = meterRegistry.config().clock()

            return Filter { next ->
                { request ->
                    val startTime = clock.monotonicTime()
                    with(next(request)) {
                        val duration = Duration.ofNanos(clock.monotonicTime() - startTime)

                        val tags = Tags.zip(config.methodName, request.method.name, config.statusName, status.code.toString())

                        Counter.builder(config.httpServerRequestsCounter.name)
                                .description(config.httpServerRequestsCounter.description)
                                .tags(tags)
                                .register(meterRegistry)
                                .increment()

                        processHandlerMetrics({ (type, path) ->
                            val formattedPath = config.pathFormatter(path)
                            when (type) {
                                HandlerMeter.Timer -> Timer.builder(config.httpServerHandlersTimer.name)
                                        .description(config.httpServerHandlersTimer.description)
                                        .tags(Tags.concat(tags, config.pathName, formattedPath))
                                        .register(meterRegistry)
                                        .record(duration)
                                HandlerMeter.Counter -> Counter.builder(config.httpServerHandlersCounter.name)
                                        .description(config.httpServerHandlersCounter.description)
                                        .tags(Tags.concat(tags, config.pathName, formattedPath))
                                        .register(meterRegistry)
                                        .increment()
                            }

                        }).cleanupMetricHeaders()
                    }
                }
            }
        }

        data class Config(
                val httpServerRequestsCounter: MeterName = MeterName("http.server.requests", "Total number of server requests"),
                val httpServerHandlersTimer: MeterName = MeterName("http.server.handlers", "Timings of server handlers"),
                val httpServerHandlersCounter: MeterName = MeterName("http.server.handlers", "Total number of handler requests"),
                val methodName: String = "method",
                val statusName: String = "status",
                val pathName: String = "path",
                val pathFormatter: (String) -> String = defaultPathFormatter
        ) {
            companion object {
                private val notAlphaNumUnderscore: Regex = "[^a-zA-Z0-9_]".toRegex()
                val defaultPathFormatter: (String) -> String = {
                    it.replace('/', '_').replace(notAlphaNumUnderscore, "")
                }
            }
        }

        enum class HandlerMeter : Filter {
            Timer {
                override fun invoke(next: HttpHandler): HttpHandler = {
                    request -> next(request).copyUriTemplateHeader(request, this)
                }
            },
            Counter {
                override fun invoke(next: HttpHandler): HttpHandler = {
                    request -> next(request).copyUriTemplateHeader(request, this)
                }
            }
        }

        private val uriTemplateHeader = "x-http4k-metrics-uri-template"
        private val meterTypeHeader = "x-http4k-metrics-type"

        private fun Response.copyUriTemplateHeader(request: Request, type: HandlerMeter) =
                replaceHeader(uriTemplateHeader, X_URI_TEMPLATE(request)).
                        replaceHeader(meterTypeHeader, Header.defaulted(meterTypeHeader, type.name)(this))

        private fun Response.processHandlerMetrics(block: (Pair<HandlerMeter, String>) -> Unit): Response {
            val type = Header.map({ HandlerMeter.valueOf(it) }).optional(meterTypeHeader)(this)
            val path = Header.optional(uriTemplateHeader)(this)
            if (type != null && path != null) {
                block(type to path)
            }
            return this
        }

        private fun Response.cleanupMetricHeaders(): Response =
                removeHeader(uriTemplateHeader).removeHeader(meterTypeHeader)
    }

    data class MeterName(val name: String, val description: String?)
}

val timer = MetricFilters.Server.HandlerMeter.Timer
val counter = MetricFilters.Server.HandlerMeter.Counter

infix fun RoutingHttpHandler.with(type: MetricFilters.Server.HandlerMeter) = withFilter(type)
