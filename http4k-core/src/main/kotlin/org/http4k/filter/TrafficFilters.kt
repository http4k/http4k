package org.http4k.filter

import org.http4k.core.Filter
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
}