package org.http4k.filter

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.filter.GzipCompressionMode.Memory
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Duration.between
import java.util.Base64

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
     * General reporting Filter for an ReportHttpTransaction. Pass an optional HttpTransactionLabeler to
     * create custom labels.
     * This is useful for logging metrics. Note that the passed function blocks the response from completing.
     */
    object ReportHttpTransaction {
        operator fun invoke(
            clock: Clock = Clock.systemUTC(),
            transactionLabeler: HttpTransactionLabeler = { it },
            recordFn: (HttpTransaction) -> Unit
        ): Filter = Filter { next ->
            {
                clock.instant().let { start ->
                    next(it).apply {
                        recordFn(transactionLabeler(HttpTransaction(it, this, between(start, clock.instant()))))
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
                recordFn(
                    "${tx.request.method}.${tx.routingGroup.replace('.', '_').replace(':', '.').replace('/', '_')}" +
                        ".${tx.response.status.code / 100}xx" +
                        ".${tx.response.status.code}", tx.duration
                )
            }
    }

    /**
     * GZipping of the response where the content-type (sans-charset) matches an allowed list of compressible types.
     */
    class GZipContentTypes(
        compressibleContentTypes: Set<ContentType>,
        private val compressionMode: GzipCompressionMode = Memory
    ) : Filter {
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

    /**
     * Some platforms deliver bodies as Base64 encoded strings.
     */
    fun Base64EncodeBody() = Filter { next ->
        { next(it).run { body(Base64.getEncoder().encodeToString(body.payload.array())) } }
    }

    class EtagSupport : Filter {
        private val md5 = ThreadLocal.withInitial {
            MessageDigest.getInstance("MD5")
        }

        override fun invoke(next: HttpHandler): HttpHandler = { request ->
            next(request).let { response ->
                if (request.method == Method.GET && response.status == Status.OK) {
                    checkEtag(request, response)
                } else response
            }
        }

        private fun checkEtag(request: Request, response: Response): Response {
            val upstreamEtag = response.header("etag")
            return if (upstreamEtag == null && isStreamingResponse(response)) {
                response
            } else {
                val etag = upstreamEtag ?: generateStrongEtag(response.body.payload.array())
                when {
                    etag == request.header("if-none-match") -> {
                        closeOldResponse(response)
                        Response(Status.NOT_MODIFIED).withSafeHeadersFrom(response)
                    }
                    upstreamEtag == null -> response.header("etag", etag)
                    else -> response
                }
            }
        }

        private fun closeOldResponse(response: Response) =
            try {
                response.close()
            } catch (ignored: Exception) {
            }

        private fun isStreamingResponse(response: Response): Boolean = response.body.length == null

        private fun Response.withSafeHeadersFrom(sourceResponse: Response) =
            SAFE_HEADERS.fold(this) { response, headerName ->
                response.headerIfPresent(
                    headerName,
                    sourceResponse.header(headerName)
                )
            }

        private fun Response.headerIfPresent(name: String, value: String?) = if (value != null) {
            this.header(name, value)
        } else {
            this
        }

        private fun generateStrongEtag(content: ByteArray): String = "\"${content.hash().toHex()}\""

        private fun ByteArray.hash(): ByteArray = md5.get().digest(this)

        private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

        companion object {
            private val SAFE_HEADERS = listOf("date", "last-modified", "cache-control", "expires", "set-cookie", "vary")
        }
    }

    /**
     * Set a Header on the response message.
     */
    fun SetHeader(name: String, value: String?) = Filter { next -> { next(it).header(name, value) } }

    /**
     * Modify response with lenses
     */
    fun Modify(vararg modifiers: (Response) -> Response): Filter = Filter { next -> { next(it).with(*modifiers) } }
}

typealias HttpTransactionLabeler = (HttpTransaction) -> HttpTransaction
