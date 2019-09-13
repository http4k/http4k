package org.http4k.events

import java.time.Clock

object EventFilters {
    fun Timed(clock: Clock) = EventsFilter { next ->
        {
            next(MetadataEvent(it) + ("timestamp" to clock.instant()))
        }
    }
}