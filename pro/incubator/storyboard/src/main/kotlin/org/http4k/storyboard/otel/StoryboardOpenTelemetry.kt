/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanLimits
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.http4k.util.OpenTelemetryClock
import java.time.Clock

fun StoryboardOpenTelemetry(store: SpanSnapshotStore = SpanSnapshotStore(), clock: Clock = Clock.systemUTC()): OpenTelemetry =
    OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setClock(OpenTelemetryClock(clock))
                .setSpanLimits(
                    SpanLimits.builder()
                        .setMaxAttributeValueLength(Int.MAX_VALUE)
                        .setMaxNumberOfAttributes(Int.MAX_VALUE)
                        .setMaxNumberOfEvents(Int.MAX_VALUE)
                        .setMaxNumberOfAttributesPerEvent(Int.MAX_VALUE)
                        .build()
                )
                .addSpanProcessor(SimpleSpanProcessor.create(StoryboardSpanExporter(store)))
                .build()
        )
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build()
