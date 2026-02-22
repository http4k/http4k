package org.http4k.filter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.thenPoly
import org.http4k.sse.SseResponse

/**
 * Adds OpenTelemetry tracing to PolyHandler.
 */
fun PolyFilters.OpenTelemetryTracing(
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get(),
    spanNamer: (Request) -> String = defaultSpanNamer,
    error: (Request, Throwable) -> String = { _, t -> t.message ?: "no message" },
    spanCreationMutator: (SpanBuilder, Request) -> SpanBuilder = { spanBuilder, _ -> spanBuilder },
    httpSpanCompletionMutator: (Span, Request, Response) -> Unit = { _, _, _ -> },
    sseSpanCompletionMutator: (Span, Request, SseResponse) -> Unit = { _, _, _ -> },
) = PolyFilter { next ->
    PolyHandler(
        http = next.http?.let {
            val openTelemetryTracing = ServerFilters.OpenTelemetryTracing(
                openTelemetry,
                spanNamer,
                error,
                spanCreationMutator,
                httpSpanCompletionMutator
            )
            openTelemetryTracing.thenPoly(it)
        },
        sse = next.sse?.let {
            ServerFilters.OpenTelemetrySseTracing(
                openTelemetry,
                spanNamer,
                error,
                spanCreationMutator,
                sseSpanCompletionMutator
            ).thenPoly(it)
        }
    )
}
