package org.http4k.filter

import org.http4k.core.*
import org.http4k.filter.GzipCompressionMode.Memory
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
     * General reporting Filter for an ReportHttpTransaction. Pass an optional HttpTransactionLabeller to
     * create custom labels.
     * This is useful for logging metrics. Note that the passed function blocks the response from completing.
     */
    object ReportHttpTransaction {
        operator fun invoke(clock: Clock = Clock.systemUTC(), transactionLabeller: HttpTransactionLabeller = { it }, recordFn: (HttpTransaction) -> Unit): Filter = Filter { next ->
            {
                clock.instant().let { start ->
                    next(it).apply {
                        recordFn(transactionLabeller(HttpTransaction(it, this, between(start, clock.instant()))))
                    }
                }
            }
        }
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
     * GZipping of the response where the content-type (sans-charset) matches an allowed list of compressible types.
     */
    class GZipContentTypes(compressibleContentTypes: Set<ContentType>, private val compressionMode: GzipCompressionMode = Memory) : Filter {
        private val compressibleMimeTypes = compressibleContentTypes
            .map { it.value }
            .map { it.split(";").first() }

        override fun invoke(next: HttpHandler): HttpHandler = { request ->
            next(request).let {
                if (requestAcceptsGzip(request) && isCompressible(it)) {
                    compressionMode.compress(it.body).apply(it)
                } else {
                    it
                }
            }
        }

        private fun isCompressible(it: Response) =
            compressibleMimeTypes.contains(mimeTypeOf(it))

        private fun mimeTypeOf(it: Response) =
            (it.header("content-type") ?: "").split(";").first().trim()

        private fun requestAcceptsGzip(it: Request) =
            (it.header("accept-encoding") ?: "").contains("gzip", true)
    }

    /**
     * Basic GZipping of Response.
     */
    object GZip {
        operator fun invoke(compressionMode: GzipCompressionMode = Memory) = Filter { next ->
            { request ->
                next(request).let {
                    if ((request.header("accept-encoding") ?: "").contains("gzip", true)) {
                        compressionMode.compress(it.body).apply(it)
                    } else it
                }
            }
        }
    }

    /**
     * Basic UnGZipping of Response.
     */
    object GunZip {
        operator fun invoke(compressionMode: GzipCompressionMode = Memory) = Filter { next ->
            { request ->
                next(request.header("accept-encoding", "gzip")).let { response ->
                    response.header("content-encoding")
                            ?.let { if (it.contains("gzip")) it else null }
                            ?.let { response.body(compressionMode.decompress(response.body)) } ?: response
                }
            }
        }
    }

}

typealias HttpTransactionLabeller = (HttpTransaction) -> HttpTransaction
