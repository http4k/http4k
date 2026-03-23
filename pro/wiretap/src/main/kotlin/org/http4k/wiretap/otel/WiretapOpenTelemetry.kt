/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME
import org.http4k.util.OpenTelemetryClock
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.TraceStore
import java.time.Clock

fun WiretapOpenTelemetry(traceStore: TraceStore,
                         logStore: LogStore,
                         clock: Clock = Clock.systemUTC(),
                         serviceName: String = "http4k-server"): OpenTelemetry {
    val resource = Resource.create(Attributes.of(SERVICE_NAME, serviceName))
    val otelClock = OpenTelemetryClock(clock)
    return OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setClock(otelClock)
                .setResource(resource)
                .addSpanProcessor(SimpleSpanProcessor.create(WiretapSpanExporter(traceStore)))
                .build()
        )
        .setLoggerProvider(
            SdkLoggerProvider.builder()
                .setClock(otelClock)
                .setResource(resource)
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(WiretapLogRecordExporter(logStore)))
                .build()
        )
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build()
}
