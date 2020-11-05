package org.http4k.filter

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.http4k.core.Filter
import org.http4k.metrics.MetricsDefaults.Companion.client
import org.http4k.metrics.MetricsDefaults.Companion.server
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.metrics.MetricsDefaults
import java.time.Clock

class MicrometerMetrics(private val defaults: MetricsDefaults) {
    fun RequestTimer(meterRegistry: MeterRegistry,
                     name: String = defaults.timerDescription.first,
                     description: String = defaults.timerDescription.second,
                     labeler: HttpTransactionLabeler = defaults.labeler,
                     clock: Clock = Clock.systemUTC()): Filter =
        ReportHttpTransaction(clock) {
            labeler(it).labels.entries.fold(Timer.builder(name).description(description)) { memo, next ->
                memo.tag(next.key, next.value)
            }.register(meterRegistry).record(it.duration)
        }

    fun RequestCounter(meterRegistry: MeterRegistry,
                       name: String = defaults.counterDescription.first,
                       description: String = defaults.counterDescription.second,
                       labeler: HttpTransactionLabeler = defaults.labeler,
                       clock: Clock = Clock.systemUTC()): Filter =
        ReportHttpTransaction(clock) {
            labeler(it).labels.entries.fold(Counter.builder(name).description(description)) { memo, next ->
                memo.tag(next.key, next.value)
            }.register(meterRegistry).increment()
        }
}

val ClientFilters.MicrometerMetrics get() = MicrometerMetrics(client)
val ServerFilters.MicrometerMetrics get() = MicrometerMetrics(server)
