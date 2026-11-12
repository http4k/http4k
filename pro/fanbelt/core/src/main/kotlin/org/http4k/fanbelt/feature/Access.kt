/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.State

data class Attempt<Actor : Any, Key : Any>(val actor: Actor, val key: Key)

sealed interface Access<out Found> {
    data class Allowed<Found>(val found: Found) : Access<Found>
    data object Hidden : Access<Nothing>
    data object Denied : Access<Nothing>
    data class Unavailable(val field: FieldName, val message: String) : Access<Nothing>
}

inline fun <Found, Next> Access<Found>.then(next: (Found) -> Access<Next>): Access<Next> = when (this) {
    is Access.Allowed -> next(found)
    Access.Hidden -> Access.Hidden
    Access.Denied -> Access.Denied
    is Access.Unavailable -> this
}

fun <Found> Access<Found>.orNull(): Found? = if (this is Access.Allowed) found else null

typealias Rule<Actor, Key, Found> = State<Attempt<Actor, Key>, Access<Found>>
