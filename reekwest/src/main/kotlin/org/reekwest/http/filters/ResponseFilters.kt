package org.reekwest.http.filters

import org.reekwest.http.core.Filter
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import java.time.Clock
import java.time.Duration
import java.time.Duration.between

object ResponseFilters {

    fun ReportLatency(clock: Clock, recordFn: (Request, Response, Duration) -> Unit): Filter = Filter {
        next ->
        {
            val start = clock.instant()
            val response = next(it)

            recordFn(it, response, between(start, clock.instant()))
            response
        }
    }
}
//object ResponseFilters {
//    /**
//     * Report the latency on a particular route to a callback function, passing the "x-reekwest-route-identity" header and response status bucket (e.g. 2xx)
//     * for identification. This is useful for logging metrics. Note that the passed function blocks the response from completing.
//     */
//    fun ReportLatency(clock: Clock, recordFn: (Request, Response, Duration) -> Unit): Filter = Filter {
//        next ->
//        {
//            val start = clock.instant()
//            val response = next(it)
//
//            recordFn(it, response, between(start, clock.instant()))
//            val identify = X_REEKWEST_ROUTE_IDENTITY(it)?.replace('.', '_')?.replace(':', '.') ?: it.method.toString() + ".UNMAPPED"
////          listOf(identify.replace('/', '_'), "${response.status.code / 100}xx", response.status.code.toString()).joinToString("."),
//            response
//        }
//    }
//}