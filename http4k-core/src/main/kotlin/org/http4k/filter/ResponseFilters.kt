package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
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
     * Basic UnGZipping of Response. Does not currently support GZipping streams
     */
    fun GZip() = Filter { next ->
        {
            val originalResponse = next(it)
            if( (it.header("accept-encoding") ?: "").contains("gzip", true)) {
                originalResponse.let {
                    val existingTransferEncodingHeader = it.header("transfer-encoding")?.let { ", " } ?: ""
                    it.body(it.body.gzipped()).replaceHeader("transfer-encoding", existingTransferEncodingHeader + "gzip")
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
                response.header("transfer-encoding")
                    ?.let { if (it.contains("gzip")) it else null }
                    ?.let { response.body(response.body.gunzipped()) } ?: response
            }
        }
    }
}
