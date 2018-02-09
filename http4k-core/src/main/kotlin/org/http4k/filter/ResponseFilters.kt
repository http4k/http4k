package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpTransaction
import org.http4k.core.Request
import org.http4k.core.Response
import java.time.Clock
import java.time.Duration
import java.time.Duration.between

object ResponseFilters {

    /**
     * Intercept the response after it is sent to the next service.
     */
    object Tap {
        operator fun invoke(fn: (Response) -> Unit) = Filter { next ->
            {
                next(it).let {
                    fn(it)
                    it
                }
            }
        }
    }

    /**
     * General reporting Filter for an ReportHttpTransaction.
     * This is useful for logging metrics. Note that the passed function blocks the response from completing.
     */
    object ReportHttpTransaction {
        operator fun invoke(clock: Clock = Clock.systemUTC(), httpTransactionGroupFormatter: HttpTransactionLabeller = { it }, recordFn: (HttpTransaction) -> Unit): Filter = Filter { next ->
            {
                clock.instant().let { start ->
                    next(it).apply {
                        val transaction = HttpTransaction(it, this, between(start, clock.instant()))
                        recordFn(httpTransactionGroupFormatter(transaction))
                    }
                }
            }
        }
    }

    /**
     * Measure and report the latency of a request to the passed function.
     */
    @Deprecated("Use ReportHttpTransaction instead", ReplaceWith("ReportHttpTransaction(clock, { tx, _ -> recordFn(tx.request, tx.response, tx.latency) })"))
    object ReportLatency {
        operator fun invoke(clock: Clock = Clock.systemUTC(), recordFn: (Request, Response, Duration) -> Unit): Filter =
            ReportHttpTransaction(clock) { tx -> recordFn(tx.request, tx.response, tx.duration) }
    }

    /**
     * Report the latency on a particular route to a callback function.
     * This is useful for logging metrics. Note that the passed function blocks the response from completing.
     */
    object ReportRouteLatency {
        operator fun invoke(clock: Clock = Clock.systemUTC(), recordFn: (String, Duration) -> Unit): Filter =
            ReportHttpTransaction(clock) { tx ->
                recordFn("${tx.request.method}.${tx.routingGroup.replace('.', '_').replace(':', '.').replace('/', '_')}" +
                    ".${tx.response.status.code / 100}xx" +
                    ".${tx.response.status.code}", tx.duration)
            }
    }

    /**
     * Basic UnGZipping of Response. Does not currently support GZipping streams
     */
    object GZip {
        operator fun invoke() = Filter { next ->
            {
                val originalResponse = next(it)
                if ((it.header("accept-encoding") ?: "").contains("gzip", true)) {
                    originalResponse.let {
                        it.body(it.body.gzipped()).replaceHeader("Content-Encoding", "gzip")
                    }
                } else originalResponse
            }
        }
    }

    /**
     * Basic UnGZipping of Response. Does not currently support GZipping streams
     */
    object GunZip {
        operator fun invoke() = Filter { next ->
            {
                next(it).let { response ->
                    response.header("content-encoding")
                        ?.let { if (it.contains("gzip")) it else null }
                        ?.let { response.body(response.body.gunzipped()) } ?: response
                }
            }
        }
    }
}

typealias HttpTransactionLabeller = (HttpTransaction) -> HttpTransaction
