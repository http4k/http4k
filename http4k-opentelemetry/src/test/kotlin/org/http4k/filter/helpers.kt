package org.http4k.filter

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import io.opentelemetry.common.Labels
import io.opentelemetry.exporters.inmemory.InMemoryMetricExporter.create
import io.opentelemetry.sdk.OpenTelemetrySdk.getMeterProvider
import io.opentelemetry.sdk.metrics.data.MetricData
import org.http4k.core.Method
import org.http4k.core.Status

/**
 * Use the InMemory exporter to get the recorded metrics from the global state.
 */
fun exportMetricsFromOpenTelemetry(): List<MetricData> = create().apply {
    export(getMeterProvider().metricProducer.collectAllMetrics())
}.finishedMetricItems

fun hasRequestTimer(count: Int, value: Double, labels: Labels, name: String = "http.server.request.latency") =
    object : Matcher<List<MetricData>> {
        override val description = name

        override fun invoke(actual: List<MetricData>): MatchResult {
            val summary = actual
                .first { it.name == name }
                .points
                .first { it.labels == labels } as MetricData.SummaryPoint
            return if (
                summary.count != count.toLong() &&
                summary.percentileValues.last().value != value
            ) MatchResult.Mismatch(actual.toString())
            else MatchResult.Match
        }
    }

fun hasRequestCounter(count: Int, labels: Labels, name: String = "http.server.request.count") =
    object : Matcher<List<MetricData>> {
        override val description = name

        override fun invoke(actual: List<MetricData>): MatchResult {
            val counter = actual
                .first { it.name == name }
                .points
                .first { it.labels == labels } as MetricData.LongPoint
            return if (counter.value == count.toLong()) MatchResult.Match else MatchResult.Mismatch(actual.toString())
        }
    }

fun hasNoRequestCounter(method: Method, path: String, status: Status) =
    object : Matcher<List<MetricData>> {
        override val description = "http.server.request.count"

        override fun invoke(actual: List<MetricData>): MatchResult =
            if (actual.find { it.name == description }
                    ?.points
                    ?.any { it.labels == Labels.of("path", path, "method", method.name, "status", status.code.toString()) } != true) MatchResult.Match else MatchResult.Mismatch(actual.toString())
    }
