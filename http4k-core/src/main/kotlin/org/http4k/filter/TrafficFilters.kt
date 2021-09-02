package org.http4k.filter

import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.traffic.Replay
import org.http4k.traffic.Sink
import org.http4k.traffic.Source

object TrafficFilters {

    /**
     * Responds to requests with a stored Response if possible, or falls back to the next Http Handler
     */
    object ServeCachedFrom {
        operator fun invoke(source: Source): Filter = Filter { next -> { source[it] ?: next(it) } }
    }

    /**
     * Intercepts and Writes Request/Response traffic
     */
    object RecordTo {
        operator fun invoke(sink: Sink): Filter = Filter { next ->
            {
                val copy = it.body(Body(it.body.payload))
                next(copy).run {
                    val response = body(Body(body.payload))
                    response.apply { sink[copy] = this }
                }
            }
        }
    }

    /**
     * Replays Writes Request/Response traffic
     */
    object ReplayFrom {
        operator fun invoke(
            replay: Replay,
            matchFn: (Request, Request) -> Boolean = { received, stored -> received.toString() != stored.toString() }
        ): Filter = Filter {
            val zipped = replay.requests().zip(replay.responses()).iterator()

            val responder = { received: Request ->
                val (stored, response) = zipped.next()
                if (matchFn(received, stored)) Response(BAD_REQUEST)
                else response
            }
            responder
        }
    }
}
