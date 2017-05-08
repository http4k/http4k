package org.http4k.http.contract

import org.http4k.http.core.Filter
import org.http4k.http.filters.ResponseFilters
import java.time.Clock
import java.time.Duration

/**
 * Report the latency on a particular route to a callback function, passing the "x-http4k-route-identity" header and response status bucket (e.g. 2xx)
 * for identification. This is useful for logging metrics. Note that the passed function blocks the response from completing.
 */
fun ResponseFilters.ReportRouteLatency(clock: Clock, recordFn: (String, Duration) -> Unit): Filter = ReportLatency(clock, {
    req, (status), duration ->
    val identify = X_REEKWEST_ROUTE_IDENTITY(req)?.replace('.', '_')?.replace(':', '.') ?: req.method.toString() + ".UNMAPPED"
    recordFn(listOf(identify.replace('/', '_'), "${status.code / 100}xx", status.code.toString()).joinToString("."), duration)
})
