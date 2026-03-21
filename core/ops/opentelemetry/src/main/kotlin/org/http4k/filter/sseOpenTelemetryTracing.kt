package org.http4k.filter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.context.Context
import org.http4k.core.Request
import org.http4k.sse.Sse
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse

/**
 * Adds OpenTelemetry tracing to SseHandler servers.
 */
fun ServerFilters.OpenTelemetrySseTracing(
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get(),
    spanNamer: (Request) -> String = defaultSpanNamer,
    error: (Request, Throwable) -> String = { _, t -> t.message ?: "no message" },
    spanCreationMutator: (SpanBuilder, Request) -> SpanBuilder = { spanBuilder, _ -> spanBuilder },
    spanCompletionMutator: (Span, Request, SseResponse) -> Unit = { _, _, _ -> },
    attributesKeys: OpenTelemetryAttributesKeys = OpenTelemetrySemanticConventions
): SseFilter {
    val context = ServerTracingContext(openTelemetry, spanNamer, error, spanCreationMutator, attributesKeys)

    return SseFilter { next ->
        { req ->
            val span = context.createServerSpan(req)

            try {
                span.makeCurrent().use {
                    val response = next(req)
                    val capturedContext = Context.current()

                    response.withConsumer { sse ->
                        capturedContext.makeCurrent().use {
                            response.consumer(object : Sse by sse {
                                override fun close() {
                                    try {
                                        span.setAttribute(attributesKeys.statusCode, response.status.code.toLong())
                                        spanCompletionMutator(span, req, response)
                                        span.setStatusFromResponse(response.status)
                                        sse.close()
                                    } finally {
                                        span.end()
                                    }
                                }
                            })
                        }
                    }
                }
            } catch (t: Throwable) {
                context.setSpanError(span, req, t)
                span.end()
                throw t
            }
        }
    }
}
