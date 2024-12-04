package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME

open class CacheControlHeaderPart(open val name: String, val value: Duration) {
    fun toHeaderValue(): String = if (value.seconds > 0) "$name=${value.seconds}" else ""
    fun replaceIn(header: String?): String = header?.let {
        header.split(",")
            .map { it.trim() }
            .filterNot { it.startsWith(name) }.plusElement(toHeaderValue())
            .joinToString(", ")
    } ?: toHeaderValue()
}

data class StaleWhenRevalidateTtl(private val valueD: Duration) :
    CacheControlHeaderPart("stale-while-revalidate", valueD)

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
    object CacheRequest {
        fun AddIfModifiedSince(clock: Clock, maxAge: Duration) = Filter { next ->
            {
                next(
                    it.replaceHeader(
                        "If-Modified-Since",
                        RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock).minus(maxAge))
                    )
                )
            }
        }
    }

    /**
     * These filters operate on Responses (post-flight)
     */
    object CacheResponse {

        private abstract class CacheFilter(private val predicate: (Response) -> Boolean) : Filter {
            abstract fun headersFor(response: Response): Headers

            override fun invoke(next: HttpHandler): HttpHandler =
                {
                    val response = next(it)
                    val headers = if (it.method == GET && predicate(response)) headersFor(response) else emptyList()
                    headers.fold(response) { memo, (first, second) -> memo.replaceHeader(first, second) }
                }
        }

        /**
         * By default, only applies when the status code of the response is < 400. This is overridable and useful -
         * For example you could combine this with a MaxAge for everything >= 400
         */
        object NoCache {
            operator fun invoke(predicate: (Response) -> Boolean = { it.status.code < 400 }): Filter =
                object : CacheFilter(predicate) {
                    override fun headersFor(response: Response) = listOf("Cache-Control" to "private, must-revalidate")
                }
        }

        /**
         * By default, only applies when the status code of the response is < 400. This is overridable.
         */
        object MaxAge {
            operator fun invoke(maxAge: Duration, predicate: (Response) -> Boolean = { it.status.code < 400 }): Filter =
                object : CacheFilter(predicate) {
                    override fun headersFor(response: Response) = listOf(
                        "Cache-Control" to listOf("public", MaxAgeTtl(maxAge).toHeaderValue()).joinToString(", ")
                    )
                }
        }

        /**
         * Applies the passed cache timings (Cache-Control, Vary) to responses, but only if they are not there already.
         * Use this for adding default cache settings.
         * By default, only applies when the status code of the response is < 400. This is overridable.
         */
        object FallbackCacheControl {
            operator fun invoke(
                defaultCacheTimings: DefaultCacheTimings,
                predicate: (Response) -> Boolean = { it.status.code < 400 }
            ): Filter {

                fun addDefaultHeaderIfAbsent(response: Response, header: String, defaultProducer: () -> String) =
                    response.replaceHeader(header, response.header(header) ?: defaultProducer())

                fun addDefaultCacheHeadersIfAbsent(response: Response) =
                    addDefaultHeaderIfAbsent(response, "Cache-Control") {
                        listOf(
                            "public",
                            defaultCacheTimings.maxAge.toHeaderValue(),
                            defaultCacheTimings.staleWhenRevalidateTtl.toHeaderValue(),
                            defaultCacheTimings.staleIfErrorTtl.toHeaderValue()
                        )
                            .filter { it != "" }
                            .joinToString(", ")
                    }
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
