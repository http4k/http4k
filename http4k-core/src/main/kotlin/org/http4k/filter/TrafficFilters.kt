package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.then
import org.http4k.filter.Traffic.Replay
import org.http4k.filter.Traffic.Source

object Responder {
    private val fallback: (Request) -> Response = { Response(SERVICE_UNAVAILABLE.description("no more traffic to replay")) }

    fun from(source: Source): HttpHandler = TrafficFilters.ServeCachedFrom(source).then(fallback)

    fun from(replay: Replay, shouldReplay: (HttpMessage) -> Boolean = { true }): HttpHandler =
        replay.responses()
            .filter(shouldReplay)
            .iterator()
            .let {
                Filter { next -> { req -> if (it.hasNext()) it.next() else next(req) } }.then(fallback)
            }
}

object Requester {
    fun from(replay: Replay): Sequence<Request> = replay.requests()
}

object TrafficFilters {

    /**
     * Responds to requests with a stored Response if possible, or falls back to the next Http Handler
     */
    fun ServeCachedFrom(source: Source): Filter = Filter { next -> { source[it] ?: next(it) } }

    /**
     * Intercepts and Writes Request/Response traffic
     */
    fun RecordTo(sink: Traffic.Sink): Filter = Filter { next -> { next(it).apply { sink[it] = this } } }
}