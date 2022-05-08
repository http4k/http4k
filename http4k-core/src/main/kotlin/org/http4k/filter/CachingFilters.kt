package org.http4k.filter

import java.nio.ByteBuffer
import org.http4k.core.Filter
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME

open class CacheControlHeaderPart(open val name: String, val value: Duration) {
    fun toHeaderValue(): String = if (value.seconds > 0) "$name=${value.seconds}" else ""
    fun replaceIn(header: String?): String? = header?.let {
        header.split(",")
            .map { it.trim() }
            .filterNot { it.startsWith(name) }.plusElement(toHeaderValue())
            .joinToString(", ")
    } ?: toHeaderValue()
}

data class StaleWhenRevalidateTtl(private val valueD: Duration) : CacheControlHeaderPart("stale-while-revalidate", valueD)

data class StaleIfErrorTtl(private val valueD: Duration) : CacheControlHeaderPart("stale-if-error", valueD)

data class MaxAgeTtl(private val valueD: Duration) : CacheControlHeaderPart("max-age", valueD)

data class DefaultCacheTimings(
    val maxAge: MaxAgeTtl,
    val staleIfErrorTtl: StaleIfErrorTtl,
    val staleWhenRevalidateTtl: StaleWhenRevalidateTtl
)

/**
 * Useful filters for applying Cache-Controls to request/responses
 */
object CachingFilters {

    /**
     * These filters operate on Requests (pre-flight)
     */
    object Request {
        fun AddIfModifiedSince(clock: Clock, maxAge: Duration) = Filter { next ->
            {
                next(it.header("If-Modified-Since", RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock).minus(maxAge))))
            }
        }
    }

    /**
     * These filters operate on Responses (post-flight)
     */
    object Response {

        private abstract class CacheFilter(private val predicate: (org.http4k.core.Response) -> Boolean) : Filter {
            abstract fun headersFor(response: org.http4k.core.Response): Headers

            override fun invoke(next: HttpHandler): HttpHandler =
                {
                    val response = next(it)
                    val headers = if (it.method == GET && predicate(response)) headersFor(response) else emptyList()
                    headers.fold(response) { memo, (first, second) -> memo.header(first, second) }
                }
        }

        /**
         * By default, only applies when the status code of the response is < 400. This is overridable and useful -
         * For example you could combine this with a MaxAge for everything >= 400
         */
        object NoCache {
            operator fun invoke(predicate: (org.http4k.core.Response) -> Boolean = { it.status.code < 400 }): Filter = object : CacheFilter(predicate) {
                override fun headersFor(response: org.http4k.core.Response) = listOf("Cache-Control" to "private, must-revalidate", "Expires" to "0")
            }
        }

        /**
         * By default, only applies when the status code of the response is < 400. This is overridable.
         */
        object MaxAge {
            operator fun invoke(clock: Clock, maxAge: Duration, predicate: (org.http4k.core.Response) -> Boolean = { it.status.code < 400 }): Filter = object : CacheFilter(predicate) {
                override fun headersFor(response: org.http4k.core.Response) = listOf(
                    "Cache-Control" to listOf("public", MaxAgeTtl(maxAge).toHeaderValue()).joinToString(", "),
                    "Expires" to RFC_1123_DATE_TIME.format(now(response).plusSeconds(maxAge.seconds))
                )

                private fun now(response: org.http4k.core.Response) =
                    try {
                        response.header("Date")?.let(RFC_1123_DATE_TIME::parse)?.let(ZonedDateTime::from)
                            ?: ZonedDateTime.now(clock)
                    } catch (e: Exception) {
                        ZonedDateTime.now(clock)
                    }
            }
        }

        /**
         * Hash algo stolen from http://stackoverflow.com/questions/26423662/scalatra-response-hmac-calulation
         * By default, only applies when the status code of the response is < 400. This is overridable.
         */
        object AddETag {

            operator fun invoke(predicate: (org.http4k.core.Response) -> Boolean = { it.status.code < 400 }): Filter = Filter { next ->
                { request ->
                    val response = next(request)
                    if (predicate(response)) {
                        val hashedBody = md5().digest(response.body.payload.copyToByteArray()).joinToString("") { "%02x".format(it) }
                        response.header("Etag", hashedBody)
                    } else
                        response
                }
            }

            private fun md5() = MessageDigest.getInstance("MD5")

            private fun ByteBuffer.copyToByteArray(): ByteArray {
                val clone = ByteBuffer.allocate(this.capacity())
                this.rewind()
                clone.put(this)
                this.rewind()
                clone.flip()
                val ba = ByteArray(clone.remaining())
                clone.get(ba)
                return ba
            }
        }

        /**
         * Applies the passed cache timings (Cache-Control, Expires, Vary) to responses, but only if they are not there already.
         * Use this for adding default cache settings.
         * By default, only applies when the status code of the response is < 400. This is overridable.
         */
        object FallbackCacheControl {
            operator fun invoke(clock: Clock, defaultCacheTimings: DefaultCacheTimings, predicate: (org.http4k.core.Response) -> Boolean = { it.status.code < 400 }): Filter {

                fun addDefaultHeaderIfAbsent(response: org.http4k.core.Response, header: String, defaultProducer: () -> String) =
                    response.replaceHeader(header, response.header(header) ?: defaultProducer())

                fun addDefaultCacheHeadersIfAbsent(response: org.http4k.core.Response) =
                    addDefaultHeaderIfAbsent(response, "Cache-Control") {
                        listOf("public", defaultCacheTimings.maxAge.toHeaderValue(), defaultCacheTimings.staleWhenRevalidateTtl.toHeaderValue(), defaultCacheTimings.staleIfErrorTtl.toHeaderValue())
                            .filter { it != "" }
                            .joinToString(", ")
                    }
                        .let { addDefaultHeaderIfAbsent(it, "Expires") { RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock).plus(defaultCacheTimings.maxAge.value)) } }
                        .let { addDefaultHeaderIfAbsent(it, "Vary") { "Accept-Encoding" } }
                return Filter { next ->
                    {
                        val response = next(it)
                        if (it.method == GET && predicate(response)) addDefaultCacheHeadersIfAbsent(response) else response
                    }
                }
            }
        }
    }
}
