/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.State

data class Attempt<Actor : Any, Key : Any>(val actor: Actor, val key: Key)

/**
 * What a rule answers about an actor and a key: `Allowed` with what it found, passed on to whatever it guards, `Hidden`
 * (as far as the actor knows it doesn't exist, a 404), `Denied` (a 403) or `Unavailable`, a `Rejected` on a field for a
 * rule about current state.
 */
sealed interface Access<out Found> {
    data class Allowed<Found>(val found: Found) : Access<Found>
    data object Hidden : Access<Nothing>
    data object Denied : Access<Nothing>
    data class Unavailable(val field: FieldName, val message: String) : Access<Nothing>
}

/**
 * Chains another check onto an allowed answer, passing on what it found; any refusal ends the chain as it is.
 */
inline fun <Found, Next> Access<Found>.then(next: (Found) -> Access<Next>): Access<Next> = when (this) {
    is Access.Allowed -> next(found)
    Access.Hidden -> Access.Hidden
    Access.Denied -> Access.Denied
    is Access.Unavailable -> this
}

/**
 * What an allowed answer found, or null for any refusal.
 */
fun <Found> Access<Found>.orNull(): Found? = if (this is Access.Allowed) found else null

/**
 * Who may be here at all: a state keyed on an `Attempt` (the actor and the key) answering `Access`, declared with `rule
 * { }` in a feature and shared by operations, regions and routes. Being a state, it is watched like one, so what it
 * read redraws whatever it guards.
 */
typealias Rule<Actor, Key, Found> = State<Attempt<Actor, Key>, Access<Found>>
