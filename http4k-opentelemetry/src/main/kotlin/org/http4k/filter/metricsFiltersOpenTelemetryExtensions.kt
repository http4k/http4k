package org.http4k.filter

import io.opentelemetry.common.Labels
import io.opentelemetry.metrics.Meter
import org.http4k.core.Filter
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.metrics.MetricsDefaults
import org.http4k.metrics.MetricsDefaults.Companion.client
import org.http4k.metrics.MetricsDefaults.Companion.server
import java.time.Clock
import java.time.Clock.systemUTC

class OpenTelemetryMetrics(private val defaults: MetricsDefaults) {

    fun RequestTimer(meter: Meter,
                     name: String = defaults.timerDescription.first,
                     description: String = defaults.timerDescription.second,
                     labeler: HttpTransactionLabeler = defaults.labeler,
                     clock: Clock = systemUTC()): Filter {
        val meterInstance = meter.longValueRecorderBuilder(name).setDescription(description).build()

        return ReportHttpTransaction(clock) { tx ->
            meterInstance.record(tx.duration.toMillis(), Labels.of(labeler.labels(tx)))
        }
    }

    fun RequestCounter(meter: Meter,
                       name: String = defaults.counterDescription.first,
                       description: String = defaults.counterDescription.second,
                       labeler: HttpTransactionLabeler = defaults.labeler,
                       clock: Clock = systemUTC()): Filter {
        val counterInstance = meter.longCounterBuilder(name).setDescription(description).build()

        return ReportHttpTransaction(clock) {
            counterInstance.add(1, Labels.of(labeler.labels(it)))
        }
    }
}

val ClientFilters.OpenTelemetryMetrics get() = OpenTelemetryMetrics(client)
val ServerFilters.OpenTelemetryMetrics get() = OpenTelemetryMetrics(server)
