package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.then
import org.http4k.filter.Traffic.Cache
import org.http4k.filter.Traffic.Recall
import org.http4k.filter.Traffic.Replay
import org.http4k.filter.Traffic.Storage

object Responder {
    private val fallback: (Request) -> Response = { Response(SERVICE_UNAVAILABLE.description("no more traffic to replay")) }

    fun from(recall: Recall): HttpHandler = TrafficFilters.ServeCachedFrom(recall).then(fallback)

    fun from(replay: Replay): HttpHandler = replay.responses().let {
        Filter { next -> { req -> if (it.hasNext()) it.next() else next(req) } }.then(fallback)
    }
}

object Requester {
    fun from(replay: Replay): Iterator<Request> = replay.requests()
}

object TrafficFilters {
    fun ServeCachedFrom(store: Recall): Filter = Filter { next -> { store[it] ?: next(it) } }

    fun RecordTo(store: Storage): Filter = Filter { next -> { next(it).apply { store[it] = this } } }

    fun SimpleCachingFrom(cache: Cache): Filter = ServeCachedFrom(cache).then(RecordTo(cache))
}