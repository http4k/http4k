package org.http4k.metrics

import io.opentelemetry.OpenTelemetry
import io.opentelemetry.metrics.Meter
import io.opentelemetry.trace.Tracer

/**
 * OpenTracing works using a set of named Singletons. We use the the constant name here to
 * make it simple to get the instances of the required objects.
 */
object Http4kOpenTelemetry {
    private const val INSTRUMENTATION_NAME = "http4k"

    val tracer: Tracer get() = OpenTelemetry.getTracer(INSTRUMENTATION_NAME)

    val meter: Meter get() = OpenTelemetry.getMeter(INSTRUMENTATION_NAME)
}
