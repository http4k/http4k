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
