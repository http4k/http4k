/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.tenancy.Workspace

data class ActorKey<Actor : Any, Key : Any>(val actor: Actor, val key: Key)

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
typealias Rule<Actor, Key, Found> = State<ActorKey<Actor, Key>, Access<Found>>

/**
 * How a refused rule is answered over HTTP, by a page or a route: its status, or null when it allows.
 */
internal fun Access<*>.refusal(): Status? = when (this) {
    is Allowed -> null
    is Refusal -> status
    is Rejected -> FORBIDDEN
}

/**
 * Binds a rule to a route: the request gives the actor and key, Allowed answers with respond and what the rule found,
 * a refusal with its status, and a key that doesn't parse is a 400.
 */
operator fun <Actor : Any, Key : Any, Found> Rule<Actor, Key, Found>.invoke(
    workspace: Workspace,
    viewer: (Request) -> ActorKey<Actor, Key>,
    respond: (Found, Request) -> Response,
): HttpHandler = { request ->
    when (val key = lensOrNull { viewer(request) }) {
        null -> Response(BAD_REQUEST)
        else -> when (val access = workspace.scope(request).read(this, key)) {
            is Allowed -> respond(access.found, request)
            else -> Response(access.refusal() ?: FORBIDDEN)
        }
    }
}
