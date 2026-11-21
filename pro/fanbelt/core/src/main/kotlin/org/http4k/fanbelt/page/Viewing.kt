/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Uri
import org.http4k.datastar.Element
import org.http4k.datastar.goTo
import org.http4k.fanbelt.feature.Access
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.Refusal
import org.http4k.fanbelt.feature.status
import org.http4k.fanbelt.feature.Rule
import org.http4k.fanbelt.feature.refusal
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.snapshot
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.lens.html

/**
 * Who is viewing a page and what they may see: actor names the viewer in traces, access is the rule they must pass to
 * see the page at all, leaveTo is where a viewer it refuses is sent, and onConnect runs when the stream opens, given
 * what the rule found (or the actor, for a page with no rule), its result closed when it ends.
 */
class Viewing<Found> private constructor(
    internal val actor: (Request) -> Any,
    private val entry: Entry<Found>,
    private val onConnect: (Scope, Request, Found) -> AutoCloseable,
    private val leaveTo: (Request) -> Uri,
) {
    internal fun read(ctx: StateContext<Unit>, request: Request) = entry.access(ctx, request)

    internal fun <R> answer(request: Request, access: Access<R>, allowed: (R) -> Response): Response = when (access) {
        is Allowed -> allowed(access.found)
        is Refusal -> Response(access.status).html(leaving(request).value)
        is Rejected -> Response(FORBIDDEN).html(leaving(request).value)
    }

    /**
     * Lets a viewer's stream in, or not.
     */
    internal fun admit(scope: Scope, request: Request): Admission {
        val gate = entry.gate(scope, request) { found -> onConnect(scope, request, found) }
        return Admission(gate.refused) { leave -> gate.open { leave(leaving(request)) } }
    }

    private fun leaving(request: Request) = Element.goTo(leaveTo(request))

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

/**
 * A viewer's stream, let in or not: refused is the status they are refused with. open runs the page's onConnect and keeps
 * checking the rule, calling leave with where to send them once it stops allowing them; closing it ends both.
 */
internal class Admission(val refused: Status?, val open: (leave: (Element) -> Unit) -> AutoCloseable)

private interface Gate {
    val refused: Status?
    fun open(leave: () -> Unit): AutoCloseable
}

private sealed interface Entry<Found> {
    val access: (StateContext<Unit>, Request) -> Access<Found>
    fun gate(scope: Scope, request: Request, connecting: (Found) -> AutoCloseable): Gate

    class Anyone<Found>(val found: (Request) -> Found) : Entry<Found> {
        override val access = { _: StateContext<Unit>, request: Request -> Allowed(found(request)) }

        override fun gate(scope: Scope, request: Request, connecting: (Found) -> AutoCloseable) = object : Gate {
            override val refused: Status? = null
            override fun open(leave: () -> Unit) = connecting(found(request))
        }
    }

    class Ruled<Found>(override val access: (StateContext<Unit>, Request) -> Access<Found>) : Entry<Found> {
        override fun gate(scope: Scope, request: Request, connecting: (Found) -> AutoCloseable): Gate = Guarded(scope, request, access, connecting)
    }
}

/**
 * Reads a viewer's access as their stream opens, and keeps reading it while the stream is open.
 */
private class Guarded<Found>(
    scope: Scope,
    request: Request,
    access: (StateContext<Unit>, Request) -> Access<Found>,
    private val connecting: (Found) -> AutoCloseable,
) : Gate {
    private val trace = Trace(scope, { presence -> scope.snapshot(presence) { ctx -> access(ctx, request) } }, ::check)

    @Volatile
    private var leave: () -> Unit = {}

    @Volatile
    private var current: Access<Found> = Denied

    override val refused: Status?
        get() = current.refusal()

    init {
        check()
    }

    override fun open(leave: () -> Unit): AutoCloseable {
        val connected = when (val allowed = current) {
            is Allowed -> connecting(allowed.found)
            else -> AutoCloseable {}
        }
        this.leave = leave
        if (refused != null) leave()
        return AutoCloseable {
            trace.close()
            connected.close()
        }
    }

    private fun check() {
        current = trace.read()
        if (refused != null) {
            trace.close()
            leave()
        }
    }
}
