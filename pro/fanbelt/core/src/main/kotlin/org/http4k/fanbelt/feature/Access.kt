/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.state.State

data class Attempt<Actor : Any, Key : Any>(val actor: Actor, val key: Key)

/**
 * A rule's answer: Allowed with what it found, or Hidden (404), Denied (403) or Rejected (a field error).
 */
sealed interface Access<out Found>

data class Allowed<Found>(val found: Found) : Access<Found>

/**
 * Transforms what an Allowed found; refusals pass through.
 */
inline fun <Found, Next> Access<Found>.map(next: (Found) -> Next): Access<Next> = flatMap { Allowed(next(it)) }

/**
 * Chains another check onto an Allowed; refusals pass through.
 */
inline fun <Found, Next> Access<Found>.flatMap(next: (Found) -> Access<Next>): Access<Next> = when (this) {
    is Allowed -> next(found)
    is Rejected -> this
    Denied -> Denied
    Hidden -> Hidden
}

/**
 * What an Allowed found, or null.
 */
fun <Found> Access<Found>.orNull(): Found? = if (this is Allowed) found else null

/**
 * A derived state that decides whether an actor may act on a key, declared with rule { }.
 */
typealias Rule<Actor, Key, Found> = State<Attempt<Actor, Key>, Access<Found>>
