package org.http4k.wiretap.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.http4k.wiretap.domain.TraceStore

fun WiretapOpenTelemetry(traceStore: TraceStore): OpenTelemetry = OpenTelemetrySdk.builder()
    .setTracerProvider(
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(WiretapSpanExporter(traceStore)))
            .build()
    )
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .build()
