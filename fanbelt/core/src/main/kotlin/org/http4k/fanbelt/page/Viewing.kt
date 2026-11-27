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
import org.http4k.fanbelt.feature.Rule
import org.http4k.fanbelt.feature.map
import org.http4k.fanbelt.feature.refusal
import org.http4k.fanbelt.feature.status
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.state.snapshot
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.lens.html

/**
 * Who is viewing a page, the page's key, and what its rule found.
 */
data class Admitted<out Actor, out Key, out Found>(val actor: Actor, val key: Key, val found: Found)

/**
 * Who is viewing a page and what they may see: actor names the viewer, key the page's subject, rule is the rule they
 * must pass to see the page at all, leaveTo is where a viewer it refuses is sent, and onConnect runs when the stream
 * opens, its result closed when it ends.
 */
class Viewing<Actor, Key, Found> private constructor(
    internal val actor: (Request) -> Actor,
    private val key: (Request) -> Key,
    private val entry: Entry<Admitted<Actor, Key, Found>>,
    private val onConnect: (Scope, Admitted<Actor, Key, Found>) -> AutoCloseable,
    private val leaveTo: (Request) -> Uri,
) {
    internal fun keyParses(request: Request) = lensOrNull { key(request); true } ?: false

    internal fun read(ctx: StateContext<Unit>, request: Request) = entry.access(ctx, request)

    internal fun <R> answer(request: Request, access: Access<R>, allowed: (R) -> Response): Response = when (access) {
        is Allowed -> allowed(access.found)
        is Refusal -> Response(access.status).html(leaving(request).value)
        is Rejected -> Response(FORBIDDEN).html(leaving(request).value)
    }

    internal fun admit(scope: Scope, request: Request) = Admission(entry.refused(scope, request)) { leave ->
        entry.gate(scope, request) { admitted -> onConnect(scope, admitted) }.open { leave(leaving(request)) }
    }

    private fun leaving(request: Request) = Element.goTo(leaveTo(request))

    companion object {
        /**
         * A page anyone in the tenant may see, with no actor.
         */
        operator fun invoke(onConnect: (Scope, Admitted<Unit, Unit, Unit>) -> AutoCloseable = { _, _ -> AutoCloseable {} }) =
            open({ }, { }, onConnect)

        /**
         * A page anyone in the tenant may see.
         */
        operator fun <Actor> invoke(
            actor: (Request) -> Actor,
            onConnect: (Scope, Admitted<Actor, Unit, Unit>) -> AutoCloseable = { _, _ -> AutoCloseable {} },
        ) = open(actor, { }, onConnect)

        /**
         * A page about a key that anyone in the tenant may see.
         */
        operator fun <Actor, Key> invoke(
            actor: (Request) -> Actor,
            key: (Request) -> Key,
            onConnect: (Scope, Admitted<Actor, Key, Unit>) -> AutoCloseable = { _, _ -> AutoCloseable {} },
        ) = open(actor, key, onConnect)

        /**
         * A page about a key that only viewers the rule allows on that key may see. A refused viewer gets 403 for
         * Denied and 404 for Hidden, and is sent to leaveTo, on arrival or when the rule stops allowing them while the
         * page is open.
         */
        operator fun <Actor : Any, Key : Any, Found> invoke(
            actor: (Request) -> Actor,
            key: (Request) -> Key,
            rule: Rule<Actor, Key, Found>,
            leaveTo: (Request) -> Uri = { Uri.of("/") },
            onConnect: (Scope, Admitted<Actor, Key, Found>) -> AutoCloseable = { _, _ -> AutoCloseable {} },
        ) = ruled(actor, key, rule, { it }, leaveTo, onConnect)

        /**
         * A page about a key that only actors the rule allows may see, for a rule on the actor alone.
         */
        @JvmName("invokeKeyedForUnitRule")
        operator fun <Actor : Any, Key, Found> invoke(
            actor: (Request) -> Actor,
            key: (Request) -> Key,
            rule: Rule<Actor, Unit, Found>,
            leaveTo: (Request) -> Uri = { Uri.of("/") },
            onConnect: (Scope, Admitted<Actor, Key, Found>) -> AutoCloseable = { _, _ -> AutoCloseable {} },
        ) = ruled(actor, key, rule, { }, leaveTo, onConnect)

        /**
         * A page only actors the rule allows may see, for a rule on the actor alone.
         */
        @JvmName("invokeForUnitRule")
        operator fun <Actor : Any, Found> invoke(
            actor: (Request) -> Actor,
            rule: Rule<Actor, Unit, Found>,
            leaveTo: (Request) -> Uri = { Uri.of("/") },
            onConnect: (Scope, Admitted<Actor, Unit, Found>) -> AutoCloseable = { _, _ -> AutoCloseable {} },
        ) = ruled(actor, { }, rule, { }, leaveTo, onConnect)

        private fun <Actor, Key> open(
            actor: (Request) -> Actor,
            key: (Request) -> Key,
            onConnect: (Scope, Admitted<Actor, Key, Unit>) -> AutoCloseable,
        ) = Viewing(actor, key, Entry.Anyone { Admitted(actor(it), key(it), Unit) }, onConnect) { Uri.of("/") }

        private fun <Actor : Any, Key, RuleKey : Any, Found> ruled(
            actor: (Request) -> Actor,
            key: (Request) -> Key,
            rule: Rule<Actor, RuleKey, Found>,
            ruleKey: (Key) -> RuleKey,
            leaveTo: (Request) -> Uri,
            onConnect: (Scope, Admitted<Actor, Key, Found>) -> AutoCloseable,
        ) = Viewing(actor, key, Entry.Ruled(rule.name.value) { ctx, request ->
            val who = actor(request)
            val about = key(request)
            ctx.read(rule, ActorKey(who, ruleKey(about))).map { Admitted(who, about, it) }
        }, onConnect, leaveTo)
    }
}

/**
 * A viewer's stream, let in or not: refused is the status they are refused with. open runs the page's onConnect and keeps
 * checking the rule, calling leave with where to send them once it stops allowing them; closing it ends both.
 */
internal class Admission(val refused: Status?, val open: (leave: (Element) -> Unit) -> AutoCloseable)

private fun interface Gate {
    fun open(leave: () -> Unit): AutoCloseable
}

private sealed interface Entry<Found> {
    val access: (StateContext<Unit>, Request) -> Access<Found>
    fun refused(scope: Scope, request: Request): Status?
    fun gate(scope: Scope, request: Request, connecting: (Found) -> AutoCloseable): Gate

    class Anyone<Found>(val found: (Request) -> Found) : Entry<Found> {
        override val access = { _: StateContext<Unit>, request: Request -> Allowed(found(request)) }

        override fun refused(scope: Scope, request: Request): Status? = null

        override fun gate(scope: Scope, request: Request, connecting: (Found) -> AutoCloseable) = Gate { connecting(found(request)) }
    }

    class Ruled<Found>(private val rule: String, override val access: (StateContext<Unit>, Request) -> Access<Found>) : Entry<Found> {
        override fun refused(scope: Scope, request: Request) = scope.reading { ctx -> access(ctx, request) }.value.refusal()

        override fun gate(scope: Scope, request: Request, connecting: (Found) -> AutoCloseable): Gate = Guarded(scope, request, rule, access, connecting)
    }
}

/**
 * Reads a viewer's access as their stream opens, and keeps reading it while the stream is open.
 */
private class Guarded<Found>(
    scope: Scope,
    request: Request,
    rule: String,
    access: (StateContext<Unit>, Request) -> Access<Found>,
    private val connecting: (Found) -> AutoCloseable,
) : Gate {
    private val trace = Trace(scope, { presence -> scope.redraw({ scope.events(AccessWaited(rule, it)) }) { scope.snapshot(presence) { ctx -> access(ctx, request) } } }, ::check)

    @Volatile
    private var leave: () -> Unit = {}

    @Volatile
    private var current: Access<Found> = Denied

    private val refused: Status?
        get() = current.refusal()

    init {
        runCatching { check() }.onFailure { trace.close() }.getOrThrow()
    }

    override fun open(leave: () -> Unit): AutoCloseable {
        val connected = runCatching {
            when (val allowed = current) {
                is Allowed -> connecting(allowed.found)
                else -> AutoCloseable {}
            }
        }.onFailure { trace.close() }.getOrThrow()
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
