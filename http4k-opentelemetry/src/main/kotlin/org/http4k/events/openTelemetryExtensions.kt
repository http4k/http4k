package org.http4k.events

import io.opentelemetry.trace.Tracer
import org.http4k.filter.SamplingDecision
import org.http4k.filter.TraceId
import org.http4k.filter.ZipkinTraces
import org.http4k.metrics.Http4kOpenTelemetry

/**
 * Adds OpenTelemetry traces metadata to the event.
 */
fun EventFilters.AddOpenTelemetryTraces(tracer: Tracer = Http4kOpenTelemetry.tracer) = EventFilter { next ->
    {
        val context = tracer.currentSpan.context
        next(it + ("traces" to ZipkinTraces(
            TraceId(context.traceIdAsHexString),
            TraceId(context.spanIdAsHexString),
            null,
            SamplingDecision(if (context.isSampled) "1" else "0")
        )))
    }
}
