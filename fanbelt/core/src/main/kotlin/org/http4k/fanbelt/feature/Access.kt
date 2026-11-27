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
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.tenancy.Scope
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
 * Reads this rule for an actor and a key, from a derived state or another rule.
 */
fun <Actor : Any, Key : Any, Found> Rule<Actor, Key, Found>.read(ctx: StateContext<*>, actor: Actor, key: Key): Access<Found> =
    read(ctx, ActorKey(actor, key))

/**
 * Reads this rule, which takes no key, for an actor, from a derived state or another rule.
 */
fun <Actor : Any, Found> Rule<Actor, Unit, Found>.read(ctx: StateContext<*>, actor: Actor): Access<Found> = read(ctx, actor, Unit)

/**
 * Reads this rule for an actor and a key in a read transaction, for background work and webhooks.
 */
fun <Actor : Any, Key : Any, Found> Rule<Actor, Key, Found>.read(scope: Scope, actor: Actor, key: Key): Access<Found> =
    read(scope, ActorKey(actor, key))

/**
 * Reads this rule, which takes no key, for an actor in a read transaction.
 */
fun <Actor : Any, Found> Rule<Actor, Unit, Found>.read(scope: Scope, actor: Actor): Access<Found> = read(scope, actor, Unit)

/**
 * How a refused rule is answered over HTTP, by a page or a route: its status, or null when it allows.
 */
internal fun Access<*>.refusal(): Status? = when (this) {
    is Allowed -> null
    is Refusal -> status
    is Rejected -> FORBIDDEN
}

/**
 * Binds a rule to a route: the request gives the actor and key, as it does for a page or an operation. Allowed answers
 * with respond, given what the rule found, the actor and the key, in the order a region's view gets them; a refusal
 * answers with its status, and an actor or key that doesn't parse is a 400.
 */
@JvmName("invokeWithActor")
operator fun <Actor : Any, Key : Any, Found> Rule<Actor, Key, Found>.invoke(
    workspace: Workspace,
    actor: (Request) -> Actor,
    key: (Request) -> Key,
    respond: (Found, Actor, Key) -> Response,
): HttpHandler = { request ->
    when (val parsed = lensOrNull { ActorKey(actor(request), key(request)) }) {
        null -> Response(BAD_REQUEST)

        else -> when (val access = workspace.scope(request).read(this, parsed)) {
            is Allowed -> respond(access.found, parsed.actor, parsed.key)
            else -> Response(access.refusal() ?: FORBIDDEN)
        }
    }
}

/**
 * Binds a rule to a route, for a handler that needs what the rule found and the key.
 */
operator fun <Actor : Any, Key : Any, Found> Rule<Actor, Key, Found>.invoke(
    workspace: Workspace,
    actor: (Request) -> Actor,
    key: (Request) -> Key,
    respond: (Found, Key) -> Response,
): HttpHandler = invoke(workspace, actor, key) { found, _: Actor, key -> respond(found, key) }

/**
 * Binds a rule with no key to a route, for a handler that needs what the rule found and the actor.
 */
@JvmName("invokeWithActorForUnitRule")
operator fun <Actor : Any, Found> Rule<Actor, Unit, Found>.invoke(
    workspace: Workspace,
    actor: (Request) -> Actor,
    respond: (Found, Actor) -> Response,
): HttpHandler = invoke(workspace, actor, { }) { found, actor: Actor, _ -> respond(found, actor) }

/**
 * Binds a rule with no key to a route, for a handler that needs only what the rule found.
 */
operator fun <Actor : Any, Found> Rule<Actor, Unit, Found>.invoke(
    workspace: Workspace,
    actor: (Request) -> Actor,
    respond: (Found) -> Response,
): HttpHandler = invoke(workspace, actor, { }) { found, _: Actor, _ -> respond(found) }
