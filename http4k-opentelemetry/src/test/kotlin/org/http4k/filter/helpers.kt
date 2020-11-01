package org.http4k.filter

import io.opentelemetry.exporters.inmemory.InMemoryMetricExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.data.MetricData

/**
 * Use the InMemory exporter to get the recorded metrics from the global state.
 */
fun exportMetricsFromOpenTelemetry(): List<MetricData> = InMemoryMetricExporter.create().apply {
    export(OpenTelemetrySdk.getMeterProvider().metricProducer.collectAllMetrics())
}.finishedMetricItems
