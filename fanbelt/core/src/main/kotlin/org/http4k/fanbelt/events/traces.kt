package org.http4k.fanbelt.events

import org.http4k.events.Event
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.page.RegionId

/**
 * A step in handling a change, for tracing.
 */
sealed interface FanbeltTrace : Event

data class Executed(val operation: String, val actor: Any?, val key: Any?, val outcome: Outcome<*>) : FanbeltTrace

data class Published(val topic: Topic) : FanbeltTrace

data class Patched(val region: RegionId, val actor: Any?, val sent: Boolean) : FanbeltTrace

data class Redrawn(val region: RegionId, val key: Any?, val changed: Boolean) : FanbeltTrace
