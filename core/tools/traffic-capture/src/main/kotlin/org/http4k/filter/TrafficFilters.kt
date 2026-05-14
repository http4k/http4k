package org.http4k.filter

import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.maxAge
import org.http4k.core.staleIfError
import org.http4k.core.staleWhileRevalidate
import org.http4k.core.then
import org.http4k.traffic.ReadWriteCache
import org.http4k.traffic.Replay
import org.http4k.traffic.Sink
import org.http4k.traffic.Source
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

object TrafficFilters {

    /**
     * Internal header names to store the expiration times of the response.
     * Removed from the response and not returned to the caller, so ensure these don't clash with real headers.
     *
     * @param expiry The header in which to store the expiration time of the response, calculated from age & max-age.
     * @param staleWhileRevalidate The header in which to store the expiration time in which a stale response can be served while revalidating.
     * @param staleIfError The header in which to store the expiration time in which a stale response can be served if an error is returned.
     * @param launchRevalidateTask If staleWhileRevalidate is configured, this is the context in which to run a revalidate task. By default, launches this in a new thread.
     */
    class CacheExpiryParams(
        val expiry: String? = "x-http4k-expiry",
        val staleWhileRevalidate: String? = "x-http4k-stale-while-revalidate",
        val staleIfError: String? = "x-http4k-stale-if-error",
        val launchRevalidateTask: (Runnable) -> Unit = { Thread(it).start() }
    )

    /**
     * Combines ServeCachedFrom and RecordTo into a single filter
     */
    object RecordToAndServeFromCache {
        operator fun invoke(
            cache: ReadWriteCache,
            params: CacheExpiryParams? = null,
            instant: () -> Instant = { Clock.System.now() },
        ): Filter = Filter { next -> ServeCachedFrom(cache).then(RecordTo(cache)).then(next)}
    }

    /**
     * Responds to requests with a stored Response if possible, or falls back to the next Http Handler
     *
     * Pass a CacheExpiryParams to only serve responses that respect the cache control directives
     */
    object ServeCachedFrom {
        operator fun invoke(
            source: Source,
            params: CacheExpiryParams? = null,
            instant: () -> Instant = { Clock.System.now() },
        ): Filter = Filter { next ->
            { request ->
                when (val responseFromCache = source[request]) {
                    null -> next(request)
                    else if params == null -> responseFromCache
                    else -> {

                        val now = instant()
                        val expiry = responseFromCache.getExpirationHeader(params.expiry)
                        val staleWhileRevalidateExpiry = responseFromCache.getExpirationHeader(params.staleWhileRevalidate)
                        val staleIfErrorExpiry = responseFromCache.getExpirationHeader(params.staleIfError)

                        when {
                            expiry > now -> responseFromCache
                            staleWhileRevalidateExpiry > now -> responseFromCache.also {
                                params.launchRevalidateTask { next(request) }
                            }

                            else -> {
                                val serverResponse = next(request)
                                if (serverResponse.status.serverError && staleIfErrorExpiry > now) {
                                    responseFromCache
                                } else {
                                    serverResponse
                                }
                            }
                        }.removeExpirationHeader(params.expiry)
                            .removeExpirationHeader(params.staleWhileRevalidate)
                            .removeExpirationHeader(params.staleIfError)
                    }
                }
            }
        }

        private fun Response.getExpirationHeader(header: String?) =
            if (header != null) {
                Instant.fromEpochMilliseconds(header(header)?.toLongOrNull() ?: 0)
            } else {
                Instant.DISTANT_PAST
            }

        private fun Response.removeExpirationHeader(header: String?) =
            if (header != null) {
                removeHeader(header)
            } else {
                this
            }
    }

    /**
     * Intercepts and Writes Request/Response traffic
     *
     * Pass a CacheExpiryParams to cache responses alongside expiration information from their cache-control directives.
     * This can then be combined with ServeCachedFrom to only serve fresh responses
     */
    object RecordTo {
        operator fun invoke(
            sink: Sink,
            params: CacheExpiryParams? = null,
            instant: () -> Instant = { Clock.System.now() },
        ): Filter = Filter { next ->
            {
                val copy = it.body(Body(it.body.payload))
                next(copy).run {
                    val response = body(Body(body.payload))
                    response.apply {
                        sink[copy] = if (params != null) {
                            val age = (header("Age")?.toLongOrNull() ?: 0).seconds
                            val maxAge = (maxAge() ?: 0).seconds
                            val staleWhileRevalidate = (staleWhileRevalidate() ?: 0).seconds
                            val staleIfError = (staleIfError() ?: 0).seconds

                            val expiry = instant().plus(maxAge).minus(age)
                            val staleWhileRevalidateExpiry = expiry.plus(staleWhileRevalidate)
                            val staleIfErrorExpiry = expiry.plus(staleIfError)

                            response
                                .addExpirationHeader(params.expiry, expiry)
                                .addExpirationHeader(params.staleWhileRevalidate, staleWhileRevalidateExpiry)
                                .addExpirationHeader(params.staleIfError, staleIfErrorExpiry)
                        } else {
                            this
                        }
                    }
                }
            }
        }

        private fun Response.addExpirationHeader(header: String?, expiry: Instant) =
            if (header != null) {
                header(header, expiry.toEpochMilliseconds().toString())
            } else {
                this
            }
    }

    /**
     * Replays Writes Request/Response traffic
     */
    object ReplayFrom {
        operator fun invoke(
            replay: Replay,
            matchFn: (Request, Request) -> Boolean = { received, stored -> received.toString() != stored.toString() }
        ): Filter {
            val pairs = replay.requests().zip(replay.responses())

            var count = 0

            return Filter {
                val responder = { received: Request ->
                    try {
                        val (req, resp) = pairs.drop(count).first()
                        if (matchFn(received, req)) Response(Status.BAD_REQUEST)
                        else resp.also { count++ }
                    } catch (e: NoSuchElementException) {
                        Response(Status.BAD_REQUEST)
                    }
                }
                responder
            }
        }
    }
}
