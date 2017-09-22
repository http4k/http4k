package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.Traffic.Cache
import org.http4k.filter.Traffic.Recall
import org.http4k.filter.Traffic.Replay
import org.http4k.filter.Traffic.Storage

object Replay {
    fun requestsFrom(replay: Replay): Iterator<Request> = replay.requests()
    fun responsesFrom(replay: Replay): HttpHandler = replay.responses().run {
        {
            if (hasNext()) next() else Response(Status.SERVICE_UNAVAILABLE.description("no more traffic to replay"))
        }
    }
}

object TrafficFilters {
    fun ServeCachedFrom(store: Recall): Filter = Filter { next -> { store[it] ?: next(it) } }

    fun RecordTo(store: Storage): Filter = Filter { next -> { next(it).apply { store[it] = this } } }

    fun SimpleCachingFrom(cache: Cache): Filter = ServeCachedFrom(cache).then(RecordTo(cache))
}