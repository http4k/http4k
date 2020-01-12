package org.http4k.filter

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
        operator fun invoke(sink: Sink): Filter = Filter { next -> { next(it).apply { sink[it] = this } } }
    }

    /**
     * Replays Writes Request/Response traffic
     */
    object ReplayFrom {
        operator fun invoke(replay: Replay): Filter = Filter {
            val zipped = replay.requests().zip(replay.responses()).iterator()

            val responder = { req: Request ->
                val (request, response) = zipped.next()
                if (req.toString() != request.toString()) Response(BAD_REQUEST)
                else response
            }
            responder
        }
    }
}