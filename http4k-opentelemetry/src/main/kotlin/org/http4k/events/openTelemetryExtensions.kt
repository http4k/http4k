package org.http4k.events

import io.opentelemetry.api.trace.Span
import org.http4k.filter.SamplingDecision
import org.http4k.filter.TraceId
import org.http4k.filter.ZipkinTraces

/**
 * Adds OpenTelemetry traces metadata to the event.
 */
fun EventFilters.AddOpenTelemetryTraces() = EventFilter { next ->
    {
        val context = Span.current().spanContext
        next(it + ("traces" to ZipkinTraces(
            TraceId(context.traceId),
            TraceId(context.spanId),
            null,
            SamplingDecision(if (context.isSampled) "1" else "0")
        )))
    }
}
