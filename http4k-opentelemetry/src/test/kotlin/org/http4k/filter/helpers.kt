package org.http4k.filter

import io.opentelemetry.exporters.inmemory.InMemoryMetricExporter
import io.opentelemetry.sdk.OpenTelemetrySdk

/**
 * Use the InMemory exporter to get the recorded metrics from the global state.
 */
fun exportMetricsFromOpenTelemetry() = InMemoryMetricExporter.create().apply {
    export(OpenTelemetrySdk.getMeterProvider().metricProducer.collectAllMetrics())
}.finishedMetricItems
