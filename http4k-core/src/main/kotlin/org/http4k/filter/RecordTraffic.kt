package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then

object Replay {
    fun requestsFrom(replay: Traffic.Replay): Iterator<Request> = replay.requests()
    fun responsesFrom(replay: Traffic.Replay): HttpHandler = replay.responses().run {
        {
            if (hasNext()) next() else Response(Status.SERVICE_UNAVAILABLE.description("no more traffic to replay"))
        }
    }
}

object ServeCachedTraffic {
    fun from(store: Traffic.Recall): Filter = Filter { next -> { store[it] ?: next(it) } }
}

object RecordTraffic {
    fun into(store: Traffic.Storage): Filter = Filter { next -> { next(it).apply { store[it] = this } } }
}

object SimpleCaching {
    fun from(cache: Traffic.Cache): Filter = ServeCachedTraffic.from(cache).then(RecordTraffic.into(cache))
}