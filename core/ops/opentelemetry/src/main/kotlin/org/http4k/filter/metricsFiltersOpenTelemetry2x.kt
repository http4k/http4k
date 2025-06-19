package org.http4k.filter

import io.opentelemetry.api.metrics.Meter
import org.http4k.core.Filter
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.metrics.Http4kOpenTelemetry
import org.http4k.metrics.OpenTelemetryMetricsDefaults
import java.time.Clock
import java.time.Clock.systemUTC

class OpenTelemetry2xMetrics(private val defaults: OpenTelemetryMetricsDefaults) {

    fun RequestDuration(
        meter: Meter = Http4kOpenTelemetry.meter,
        clock: Clock = systemUTC(),
    ): Filter {
        val meterInstance = meter.histogramBuilder(defaults.metricsDescription.first)
            .setExplicitBucketBoundariesAdvice(defaults.bucketBoundaryAdvice)
            .setDescription(defaults.metricsDescription.second)
            .setUnit("s")
            .build()

        return ReportHttpTransaction(clock) { tx ->
            meterInstance.record(tx.duration.toMillis() / 1000.0, defaults.metricsLabeler(tx))
        }
    }

    companion object
}

val ClientFilters.OpenTelemetry2xMetrics get() = OpenTelemetry2xMetrics(OpenTelemetryMetricsDefaults.client)
val ServerFilters.OpenTelemetry2xMetrics get() = OpenTelemetry2xMetrics(OpenTelemetryMetricsDefaults.server)
