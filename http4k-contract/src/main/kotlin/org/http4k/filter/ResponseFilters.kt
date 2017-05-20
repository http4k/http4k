package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.lens.Header.X_URI_TEMPLATE
import java.time.Clock
import java.time.Duration

/**
 * Report the latency on a particular route to a callback function, passing the "x-uri-template" header and response status bucket (e.g. 2xx)
 * for identification. This is useful for logging metrics. Note that the passed function blocks the response from completing.
 */
fun ResponseFilters.ReportRouteLatency(clock: Clock, recordFn: (String, Duration) -> Unit): Filter = ReportLatency(clock, {
    req, response, duration ->
    val identify = req.method.toString() + "." + (X_URI_TEMPLATE(req)?.replace('.', '_')?.replace(':', '.') ?: "UNMAPPED")
        recordFn (listOf(identify.replace('/', '_'), "${response.status.code / 100}xx", response.status.code.toString()).joinToString("."), duration)
})
