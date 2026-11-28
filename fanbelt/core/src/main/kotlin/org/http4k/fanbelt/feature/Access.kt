package org.http4k.fanbelt.feature

import org.http4k.core.Status
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.state.State

data class ActorKey<Actor : Any, Key : Any>(val actor: Actor, val key: Key)

/**
 * A rule's answer: Allowed with what it found, or a refusal.
 */
sealed interface Access<out Found>

data class Allowed<Found>(val found: Found) : Access<Found>

inline fun <Found, Next> Access<Found>.map(next: (Found) -> Next): Access<Next> = flatMap { Allowed(next(it)) }

inline fun <Found, Next> Access<Found>.flatMap(next: (Found) -> Access<Next>): Access<Next> = when (this) {
    is Allowed -> next(found)
    is Rejected -> this
    Denied -> Denied
    Hidden -> Hidden
}

fun <Found> Access<Found>.orNull(): Found? = if (this is Allowed) found else null

/**
 * A derived state that decides whether an actor may act on a key.
 */
typealias Rule<Actor, Key, Found> = State<ActorKey<Actor, Key>, Access<Found>>

internal fun Access<*>.refusal(): Status? = when (this) {
    is Allowed -> null
    is Refusal -> status
    is Rejected -> FORBIDDEN
}
