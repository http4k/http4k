package org.http4k.filter

import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.B3Propagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter.create
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor

val inMemorySpanExporter = create()

fun setupOtelSdk() = OpenTelemetrySdk.builder()
    .setTracerProvider(
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
            .build()
    )
    .setPropagators(ContextPropagators.create(B3Propagator.injectingMultiHeaders()))
    .build()
