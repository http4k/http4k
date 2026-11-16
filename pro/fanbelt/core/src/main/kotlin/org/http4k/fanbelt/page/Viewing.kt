/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.fanbelt.feature.Access
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Rule
import org.http4k.fanbelt.feature.refusal
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.tenancy.Scope

/**
 * Who is viewing a page and what they may see: actor names the viewer in traces, access is the rule they must pass to
 * see the page at all, leaveTo is where a viewer it refuses is sent, and onConnect runs when the stream opens, given
 * what the rule found (or the actor, for a page with no rule), its result closed when it ends.
 */
class Viewing<Found> private constructor(
    internal val actor: (Request) -> Any,
    private val entry: Entry<Found>,
    private val onConnect: (Scope, Request, Found) -> AutoCloseable,
    internal val leaveTo: (Request) -> Uri,
) {
    internal fun refusal(ctx: StateContext<Unit>, request: Request) = when (entry) {
        is Entry.Anyone -> null
        is Entry.Ruled -> entry.access(ctx, request).refusal()
    }

    internal fun admit(scope: Scope, request: Request): Admission = when (entry) {
        is Entry.Anyone -> Admission.Open { onConnect(scope, request, entry.found(request)) }
        is Entry.Ruled -> Admission.Guarded(scope, request, entry.access) { found -> onConnect(scope, request, found) }
    }

    companion object {
        /**
         * A page anyone in the tenant may see, with no actor.
         */
        operator fun invoke(onConnect: (Scope, Request, Unit) -> AutoCloseable = { _, _, _ -> AutoCloseable {} }) =
            Viewing({ }, onConnect)

        /**
         * A page anyone in the tenant may see. onConnect is given the actor.
         */
        operator fun <Actor : Any> invoke(
            actor: (Request) -> Actor,
            onConnect: (Scope, Request, Actor) -> AutoCloseable = { _, _, _ -> AutoCloseable {} },
        ) = Viewing(actor, Entry.Anyone(actor), onConnect) { Uri.of("/") }

        /**
         * A page only viewers the rule allows may see. A refused viewer gets 403 for Denied and 404 for Hidden, and is
         * sent to leaveTo, on arrival or when the rule stops allowing them while the page is open. onConnect is given
         * what the rule found.
         */
        operator fun <Actor : Any, Key : Any, Found> invoke(
            viewer: (Request) -> ActorKey<Actor, Key>,
            access: Rule<Actor, Key, Found>,
            leaveTo: (Request) -> Uri = { Uri.of("/") },
            onConnect: (Scope, Request, Found) -> AutoCloseable = { _, _, _ -> AutoCloseable {} },
        ) = Viewing({ viewer(it).actor }, Entry.Ruled { ctx, request -> ctx.read(access, viewer(request)) }, onConnect, leaveTo)

        /**
         * A page only actors the rule allows may see, for a rule on the actor alone.
         */
        @JvmName("invokeForUnitRule")
        operator fun <Actor : Any, Found> invoke(
            actor: (Request) -> Actor,
            access: Rule<Actor, Unit, Found>,
            leaveTo: (Request) -> Uri = { Uri.of("/") },
            onConnect: (Scope, Request, Found) -> AutoCloseable = { _, _, _ -> AutoCloseable {} },
        ) = Viewing(actor, Entry.Ruled { ctx, request -> ctx.read(access, ActorKey(actor(request), Unit)) }, onConnect, leaveTo)
    }
}

private sealed interface Entry<Found> {
    class Anyone<Found>(val found: (Request) -> Found) : Entry<Found>
    class Ruled<Found>(val access: (StateContext<Unit>, Request) -> Access<Found>) : Entry<Found>
}
