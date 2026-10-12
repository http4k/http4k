package org.http4k.filter

import org.http4k.base64DecodedByteBuffer
import org.http4k.core.Body
import org.http4k.core.ContentEncoding
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.REQUEST_ENTITY_TOO_LARGE
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.filter.GzipCompressionMode.Memory
import org.http4k.util.SizeLimitExceededException

object RequestFilters {

    /**
     * Intercept the request before it is sent to the next service.
     */
    object Tap {
        operator fun invoke(fn: (Request) -> Unit) = Filter { next ->
            {
                fn(it)
                next(it)
            }
        }
    }

    /**
     * Basic GZipping of Request.
     */
    object GZip {
        @Deprecated(
            USE_COMPRESSION_FILTERS,
            ReplaceWith(
                "RequestFilters.Compression(ContentEncoding.Gzip())",
                "org.http4k.core.ContentEncoding",
                "org.http4k.core.Gzip"
            )
        )
        operator fun invoke(compressionMode: GzipCompressionMode = Memory()) = Filter { next ->
            {
                next(compressionMode.compress(it.body).apply(it))
            }
        }
    }

    /**
     * Client-side: compresses the Request body with the given encoding, which the server must support.
     */
    object Compression {
        operator fun invoke(encoding: ContentEncoding) = Filter { next ->
            { request ->
                next(encoding.encoded(request.body).apply(request))
            }
        }
    }

    /**
     * Basic UnGZipping of Request.
     */
    object GunZip {
        @Deprecated(
            USE_COMPRESSION_FILTERS,
            ReplaceWith(
                "RequestFilters.Decompression(ContentEncoding.Gzip(), maxDecompressedSize = maxDecompressedSize)",
                "org.http4k.core.ContentEncoding",
                "org.http4k.core.Gzip"
            )
        )
        operator fun invoke(
            compressionMode: GzipCompressionMode = Memory(),
            maxDecompressedSize: Long = MAX_DECOMPRESSED_SIZE.toLong()
        ) = Filter { next ->
            { request ->
                request.header("content-encoding")
                    ?.let { if (it.contains("gzip")) it else null }
                    ?.let {
                        try {
                            next(request.body(compressionMode.decompress(request.body, maxDecompressedSize)))
                        } catch (_: SizeLimitExceededException) {
                            Response(REQUEST_ENTITY_TOO_LARGE)
                        }
                    } ?: next(request)
            }
        }
    }

    /**
     * Decompresses the Request body when its "content-encoding" matches one of the given encodings.
     */
    object Decompression {
        operator fun invoke(
            vararg encodings: ContentEncoding,
            maxDecompressedSize: Long = MAX_DECOMPRESSED_SIZE.toLong()
        ) = Filter { next ->
            { request ->
                request.decodedWith(encodings, maxDecompressedSize)
                    ?.let {
                        try {
                            next(it)
                        } catch (_: SizeLimitExceededException) {
                            Response(REQUEST_ENTITY_TOO_LARGE)
                        } catch (_: DecompressionFailure) {
                            Response(BAD_REQUEST)
                        }
                    } ?: next(request)
            }
        }
    }

    enum class ProxyProtocolMode(private val fn: (Uri) -> Uri) {
        Http({ it.scheme("http") }),
        Https({ it.scheme("https") }),
        Port({
            when (it.port) {
                443 -> Https(it)
                else -> Http(it)
            }
        });

        operator fun invoke(uri: Uri) = fn(uri)
    }

    /**
     * Sets the host on an outbound request from the Host header of the incoming request. This is useful for implementing proxies.
     * Note the use of the ProxyProtocolMode to set the outbound scheme
     */
    object ProxyHost {
        operator fun invoke(mode: ProxyProtocolMode = ProxyProtocolMode.Http): Filter = Filter { next ->
            {
                it.header("Host")?.let { host -> next(it.uri(mode(it.uri).authority(host))) }
                    ?: Response(BAD_REQUEST.description("Cannot proxy without host header"))
            }
        }
    }

    /**
     * Some platforms deliver bodies as Base64 encoded strings.
     */
    fun Base64DecodeBody() = Filter { next ->
        { next(it.body(Body(it.body.payload.base64DecodedByteBuffer()))) }
    }

    /**
     * Set a Header on the request message.
     */
    fun SetHeader(name: String, value: String?) = Filter { next -> { next(it.header(name, value)) } }

    /**
     * Modify request with lenses
     */
    fun Modify(vararg modifiers: (Request) -> Request): Filter = Filter { next -> { next(it.with(*modifiers)) } }

    /**
     * Only include headers matching the given names
     */
    fun IncludeHeaders(vararg headerNames: String) = Filter { next ->
        {
            next(it.replaceHeaders(it.headers.filter { (key, _) -> headerNames.contains(key) }))
        }
    }

    /**
     * Exclude headers matching the given names
     */
    fun ExcludeHeaders(vararg headerNames: String) = Filter { next ->
        {
            next(it.replaceHeaders(it.headers.filter { (key, _) -> !headerNames.contains(key) }))
        }
    }
}
