package org.http4k.filter

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.common.Labels
import org.http4k.core.Filter
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.metrics.Http4kOpenTelemetry
import org.http4k.metrics.MetricsDefaults
import org.http4k.metrics.MetricsDefaults.Companion.client
import org.http4k.metrics.MetricsDefaults.Companion.server
import java.time.Clock
import java.time.Clock.systemUTC

class OpenTelemetryMetrics(private val defaults: MetricsDefaults) {

    fun RequestTimer(meter: Meter = Http4kOpenTelemetry.meter,
                     name: String = defaults.timerDescription.first,
                     description: String = defaults.timerDescription.second,
                     labeler: HttpTransactionLabeler = defaults.labeler,
                     clock: Clock = systemUTC()): Filter {
        val meterInstance = meter.longValueRecorderBuilder(name)
            .setDescription(description).setUnit("ms").build()

        return ReportHttpTransaction(clock) { tx ->
            val labels = labeler(tx).labels.flatMap { listOf(it.key, it.value) }.toTypedArray()
            meterInstance.record(tx.duration.toMillis(), Labels.of(*labels))
        }
    }

    fun RequestCounter(meter: Meter = Http4kOpenTelemetry.meter,
                       name: String = defaults.counterDescription.first,
                       description: String = defaults.counterDescription.second,
                       labeler: HttpTransactionLabeler = defaults.labeler,
                       clock: Clock = systemUTC()): Filter {
        val counterInstance = meter.longCounterBuilder(name)
            .setDescription(description).setUnit("1").build()

        return ReportHttpTransaction(clock) { tx ->
            val labels = labeler(tx).labels.flatMap { listOf(it.key, it.value) }.toTypedArray()
            counterInstance.add(1, Labels.of(*labels))
        }
    }
}

val ClientFilters.OpenTelemetryMetrics get() = OpenTelemetryMetrics(client)
val ServerFilters.OpenTelemetryMetrics get() = OpenTelemetryMetrics(server)
