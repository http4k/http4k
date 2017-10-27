package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.Header
import java.time.Clock
import java.time.Duration
import java.time.Duration.between

object ResponseFilters {

    /**
     * Intercept the response after it is sent to the next service.
     */
    fun Tap(fn: (Response) -> Unit) = Filter { next ->
        {
            next(it).let {
                fn(it)
                it
            }
        }
    }

    /**
     * Measure and report the latency of a request to the passed function.
     */
    fun ReportLatency(clock: Clock = Clock.systemUTC(), recordFn: (Request, Response, Duration) -> Unit): Filter = Filter { next ->
        {
            val start = clock.instant()
            val response = next(it)

            recordFn(it, response, between(start, clock.instant()))
            response
        }
    }

    /**
     * Report the latency on a particular route to a callback function, passing the "x-uri-template" header and response status bucket (e.g. 2xx)
     * for identification. This is useful for logging metrics. Note that the passed function blocks the response from completing.
     */
    fun ReportRouteLatency(clock: Clock, recordFn: (String, Duration) -> Unit): Filter = ReportLatency(clock, { req, response, duration ->
        val identify = req.method.toString() + "." + (Header.X_URI_TEMPLATE(req)?.replace('.', '_')?.replace(':', '.') ?: "UNMAPPED")
        recordFn(listOf(identify.replace('/', '_'), "${response.status.code / 100}xx", response.status.code.toString()).joinToString("."), duration)
    })


    /**
     * Basic UnGZipping of Response. Does not currently support GZipping streams
     */
    fun GZip() = Filter { next ->
        {
            val originalResponse = next(it)
            if ((it.header("accept-encoding") ?: "").contains("gzip", true)) {
                originalResponse.let {
                    it.body(it.body.gzipped()).replaceHeader("Content-Encoding", "gzip")
                }
            } else originalResponse
        }
    }

    /**
     * Basic UnGZipping of Response. Does not currently support GZipping streams
     */
    fun GunZip() = Filter { next ->
        {
            next(it).let { response ->
                response.header("content-encoding")
                    ?.let { if (it.contains("gzip")) it else null }
                    ?.let { response.body(response.body.gunzipped()) } ?: response
            }
        }
    }
}
