package org.http4k.filter

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.http4k.core.Method
import org.http4k.core.Status
import java.time.Duration

/**
 * Use the InMemory exporter to get the recorded metrics from the global state.
 */
val inMemoryMetricExporter: InMemoryMetricReader = InMemoryMetricReader.create()

fun setupOpenTelemetryMeterProvider() {
    inMemoryMetricExporter.collectAllMetrics()
    OpenTelemetrySdk.builder()
        .setMeterProvider(
            SdkMeterProvider.builder()
                .registerMetricReader(inMemoryMetricExporter)
                .setMinimumCollectionInterval(Duration.ofMillis(1)).build())
        .buildAndRegisterGlobal()
}

fun exportMetricsFromOpenTelemetry(): List<MetricData> =
    inMemoryMetricExporter.collectAllMetrics().toList()

fun hasRequestTimer(count: Int, value: Double, attributes: Attributes, name: String = "http.server.request.latency") =
    object : Matcher<List<MetricData>> {
        override val description = name

        override fun invoke(actual: List<MetricData>): MatchResult {
            val summary = actual
                .first { it.name == name }
                .doubleHistogramData
                .points
                .first { it.attributes == attributes }
            return if (
                summary.count != count.toLong() &&
                summary.epochNanos - summary.startEpochNanos == value.toLong()
            ) MatchResult.Mismatch(actual.toString())
            else MatchResult.Match
        }
    }

fun hasRequestCounter(count: Int, attributes: Attributes, name: String = "http.server.request.count") =
    object : Matcher<List<MetricData>> {
        override val description = name

        override fun invoke(actual: List<MetricData>): MatchResult {
            val counter = actual
                .first { it.name == name }
                .longSumData
                .points
                .first {
                    it.attributes == attributes
                }
            return if (counter.value == count.toLong()) MatchResult.Match else MatchResult.Mismatch(actual.toString())
        }
    }

fun hasNoRequestCounter(method: Method, path: String, status: Status) =
    object : Matcher<List<MetricData>> {
        override val description = "http.server.request.count"

        override fun invoke(actual: List<MetricData>): MatchResult =
            if (actual.find { it.name == description }
                    ?.longSumData
                    ?.points
                    ?.any {
                        it.attributes == Attributes.of(
                            AttributeKey.stringKey("path"),
                            path,
                            AttributeKey.stringKey("method"),
                            method.name,
                            AttributeKey.stringKey("status"),
                            status.code.toString()
                        )
                    } != true) MatchResult.Match else MatchResult.Mismatch(actual.toString())
    }
