package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then

object Replay {
    fun requestsFrom(replay: TrafficReplay): Iterator<Request> = replay.requests()
    fun responsesFrom(replay: TrafficReplay): HttpHandler = replay.responses().run {
        {
            if (hasNext()) next()
            else Response(Status.SERVICE_UNAVAILABLE.description("no more traffic to replay"))
        }
    }
}

object ServeCachedTraffic {
    fun from(store: TrafficRecall): Filter = Filter { next ->
        {
            store[it] ?: next(it)
        }
    }
}

object RecordTraffic {
    fun into(store: TrafficStorage): Filter = Filter { next ->
        {
            next(it).apply {
                store[it] = this
            }
        }
    }
}

object SimpleCaching {
    fun from(cache: TrafficCache): Filter {
        return ServeCachedTraffic.from(cache).then(RecordTraffic.into(cache))
    }
}