package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.then
import org.http4k.filter.Traffic.Storage
import org.http4k.filter.Traffic.TrafficStream

object Responder {
    private val fallback: (Request) -> Response = { Response(SERVICE_UNAVAILABLE.description("no more traffic to replay")) }

    fun from(storage: Storage): HttpHandler = TrafficFilters.ServeCacheFrom(storage).then(fallback)

    fun from(trafficStream: TrafficStream,
             shouldReplay: (HttpMessage) -> Boolean = { true }): HttpHandler =
        trafficStream.responses()
            .filter(shouldReplay)
            .iterator()
            .let {
                Filter { next -> { req -> if (it.hasNext()) it.next() else next(req) } }.then(fallback)
            }
}

object Requester {
    fun from(trafficStream: TrafficStream): Sequence<Request> = trafficStream.requests()
}

object TrafficFilters {

    /**
     * Responds to requests with a stored Response if possible, or falls back to the next Http Handler
     */
    fun ServeCacheFrom(storage: Storage): Filter = Filter { next -> { storage[it] ?: next(it) } }

    /**
     * Intercepts and stores Request/Response traffic in the Storage
     */
    fun RecordTo(storage: Storage): Filter = Filter { next -> { next(it).apply { storage[it] = this } } }
}