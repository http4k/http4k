package org.http4k.fanbelt.events

import org.http4k.events.Event

/**
 * A failure inside Fanbelt, with its cause.
 */
interface FanbeltFailure : Event {
    val cause: Throwable
}

data class RequestFailed(override val cause: Throwable) : FanbeltFailure

data class StreamFailed(override val cause: Throwable) : FanbeltFailure
