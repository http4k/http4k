package org.http4k.metrics

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer

/**
 * OpenTracing works using a set of named Singletons. We use the the constant name here to
 * make it simple to get the instances of the required objects.
 */
object Http4kOpenTelemetry {
    private const val INSTRUMENTATION_NAME = "http4k"

    val tracer: Tracer get() = OpenTelemetry.getGlobalTracer(INSTRUMENTATION_NAME)

    val meter: Meter get() = OpenTelemetry.getGlobalMeter(INSTRUMENTATION_NAME)
}
