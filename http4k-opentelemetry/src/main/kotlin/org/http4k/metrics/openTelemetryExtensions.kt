package org.http4k.metrics

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.GlobalMeterProvider
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer

/**
 * OpenTracing works using a set of named Singletons. We use the the constant name here to
 * make it simple to get the instances of the required objects.
 */
object Http4kOpenTelemetry {
    const val INSTRUMENTATION_NAME = "http4k"

    val tracer: Tracer get() = GlobalOpenTelemetry.get().tracerProvider.get(INSTRUMENTATION_NAME)

    val meter: Meter get() = GlobalMeterProvider.getMeter(INSTRUMENTATION_NAME)
}
