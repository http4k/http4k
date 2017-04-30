package org.reekwest.http.contract.filters

import org.reekwest.http.contract.X_REEKWEST_ROUTE_IDENTITY
import org.reekwest.http.core.Filter
import java.time.Clock
import java.time.Duration
import java.time.Duration.between

object ResponseFilters {

    /**
     * Report the latency on a particular route to a callback function, passing the "x-reekwest-route-identity" header and response status bucket (e.g. 2xx)
     * for identification. This is useful for logging metrics. Note that the passed function blocks the response from completing.
     */
    fun ReportLatency(clock: Clock, recordFn: (String, Duration) -> Unit): Filter = Filter {
        next ->
        {
            val start = clock.instant()
            val response = next(it)
            val identify = X_REEKWEST_ROUTE_IDENTITY(it)?.replace('.', '_')?.replace(':', '.') ?: it.method.toString() + ".UNMAPPED"

            recordFn(
                listOf(identify.replace('/', '_'), "${response.status.code / 100}xx", response.status.code.toString()).joinToString("."),
                between(start, clock.instant())
            )
            response
        }
    }
}