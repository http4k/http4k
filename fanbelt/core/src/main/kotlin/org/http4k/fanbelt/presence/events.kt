package org.http4k.fanbelt.presence

import org.http4k.events.Event
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.events.FanbeltFailure

data class PresenceRenewalFailed(override val cause: Throwable) : FanbeltFailure

data object PresenceRecovered : Event

data class PresenceJoinFailed(val topic: Topic, override val cause: Throwable) : FanbeltFailure

data class CarryRefused(val presence: String, val actor: Any?, val key: Any?) : Event
