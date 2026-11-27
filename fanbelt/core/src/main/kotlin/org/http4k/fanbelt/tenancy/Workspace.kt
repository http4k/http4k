package org.http4k.fanbelt.tenancy

import dev.forkhandles.values.Value
import kotlin.reflect.KProperty1
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.core.appendToPath
import org.http4k.core.then
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Operation
import org.http4k.fanbelt.feature.Rule
import org.http4k.fanbelt.feature.refusal
import org.http4k.fanbelt.feature.resets
import org.http4k.fanbelt.http.Guard
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.input.ErrorSignals
import org.http4k.fanbelt.input.Feedback
import org.http4k.fanbelt.input.SignalsJson.datastarModel
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.fanbelt.state.PresenceState
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.routing.poly

/**
 * Routes each request to its tenant's Scope, by path variable or the app's own tenant and root functions. A tenant that
 * exists rejects is a 404.
 */
class Workspace(
    internal val infra: AppInfra,
    private val template: String,
    private val exists: (TenantId) -> Boolean,
    private val tenant: (Request) -> TenantId,
    private val root: (TenantId) -> Uri,
) {
    /**
     * Takes the tenant from the template's path variable, such as `/{tenant}`, and roots each tenant's pages there.
     */
    constructor(infra: AppInfra, template: String, exists: (TenantId) -> Boolean) : this(
        infra,
        template,
        exists,
        Path.value(TenantId).of(variableIn(template)),
        { tenant -> Uri.of(variable.replace(template, tenant.value)).appendToPath("/") },
    )

    /**
     * Mounts the tenant's routes under the template, answering 404 for a tenant that doesn't exist.
     */
    fun routes(tenantRoutes: PolyHandler) = poly(template bind knownTenant.then(tenantRoutes))

    /**
     * The Scope of the request's tenant.
     */
    internal fun scope(request: Request) = infra.scope(tenant(request))

    /**
     * The root of the request's tenant, for links and redirects.
     */
    fun uri(request: Request) = uri(tenant(request))

    /**
     * The root of a tenant, for links and redirects.
     */
    fun uri(tenant: TenantId): Uri = root(tenant)

    /**
     * A route that reads a rule: the request gives the actor and key, as it does for a page. Allowed answers with
     * respond, given what the rule found, the actor and the key, in the order a region's view gets them; a refusal
     * answers with its status, and an actor or key that doesn't parse is a 400.
     */
    fun <Actor : Any, Key : Any, Found> read(
        rule: Rule<Actor, Key, Found>,
        actor: (Request) -> Actor,
        key: (Request) -> Key,
        respond: (Found, Actor, Key) -> Response,
    ): HttpHandler = { request ->
        when (val parsed = lensOrNull { ActorKey(actor(request), key(request)) }) {
            null -> Response(BAD_REQUEST)
            else -> when (val access = scope(request).read(rule, parsed)) {
                is Allowed -> respond(access.found, parsed.actor, parsed.key)
                else -> Response(access.refusal() ?: FORBIDDEN)
            }
        }
    }

    /**
     * A route that reads a rule, for a handler that needs what it found and the key.
     */
    fun <Actor : Any, Key : Any, Found> read(
        rule: Rule<Actor, Key, Found>,
        actor: (Request) -> Actor,
        key: (Request) -> Key,
        respond: (Found, Key) -> Response,
    ): HttpHandler = read(rule, actor, key) { found, _: Actor, key -> respond(found, key) }

    /**
     * A route that reads a rule with no key, for a handler that needs what it found and the actor.
     */
    fun <Actor : Any, Found> read(
        rule: Rule<Actor, Unit, Found>,
        actor: (Request) -> Actor,
        respond: (Found, Actor) -> Response,
    ): HttpHandler = read(rule, actor, { }) { found, actor: Actor, _ -> respond(found, actor) }

    /**
     * A route that reads a rule with no key, for a handler that needs only what it found.
     */
    fun <Actor : Any, Found> read(
        rule: Rule<Actor, Unit, Found>,
        actor: (Request) -> Actor,
        respond: (Found) -> Response,
    ): HttpHandler = read(rule, actor, { }) { found, _: Actor, _ -> respond(found) }

    /**
     * A POST that executes an operation, building its input from the request's signals and the key the request gives,
     * such as a path value. Signals named in reset go back to their defaults once it's accepted. Input that doesn't
     * parse is a 400, Denied a 403, Hidden a 404, and accepted or rejected outcomes go to the Feedback.
     */
    inline fun <Actor, reified Signals : Any, Key, Input, Result : Any> execute(
        operation: Operation<Actor, Input, Result>,
        noinline actor: (Request) -> Actor,
        noinline key: (Request) -> Key,
        reset: Set<KProperty1<Signals, *>> = emptySet(),
        feedback: Feedback<Result> = ErrorSignals(),
        crossinline input: (Signals, Key) -> Input,
    ): HttpHandler = operation.handler(this, feedback, resets(reset)) { request -> actor(request) to input(request.datastarModel<Signals>(), key(request)) }

    /**
     * A POST that executes an operation, building its input from the request's signals alone.
     */
    inline fun <Actor, reified Signals : Any, Input, Result : Any> execute(
        operation: Operation<Actor, Input, Result>,
        noinline actor: (Request) -> Actor,
        reset: Set<KProperty1<Signals, *>> = emptySet(),
        feedback: Feedback<Result> = ErrorSignals(),
        crossinline input: (Signals) -> Input,
    ): HttpHandler = operation.handler(this, feedback, resets(reset)) { request -> actor(request) to input(request.datastarModel<Signals>()) }

    /**
     * A POST that executes an operation, taking its input from the request.
     */
    fun <Actor, Input, Result : Any> execute(
        operation: Operation<Actor, Input, Result>,
        actor: (Request) -> Actor,
        feedback: Feedback<Result> = ErrorSignals(),
        input: (Request) -> Input,
    ): HttpHandler = operation.handler(this, feedback, emptyMap()) { request -> actor(request) to input(request) }

    /**
     * A POST that executes an operation with no actor, from the request's signals and the key it gives.
     */
    @JvmName("executeWithNoActor")
    inline fun <reified Signals : Any, Key, Input, Result : Any> execute(
        operation: Operation<Unit, Input, Result>,
        noinline key: (Request) -> Key,
        reset: Set<KProperty1<Signals, *>> = emptySet(),
        feedback: Feedback<Result> = ErrorSignals(),
        crossinline input: (Signals, Key) -> Input,
    ): HttpHandler = execute(operation, { _: Request -> }, key, reset, feedback, input)

    /**
     * A POST that executes an operation with no actor, from the request's signals alone.
     */
    @JvmName("executeWithNoActorOrKey")
    inline fun <reified Signals : Any, Input, Result : Any> execute(
        operation: Operation<Unit, Input, Result>,
        reset: Set<KProperty1<Signals, *>> = emptySet(),
        feedback: Feedback<Result> = ErrorSignals(),
        crossinline input: (Signals) -> Input,
    ): HttpHandler = execute(operation, { _: Request -> }, reset, feedback, input)

    /**
     * A POST that executes an operation with no actor, taking its input from the request.
     */
    @JvmName("executeWithNoActorFromRequest")
    fun <Input, Result : Any> execute(
        operation: Operation<Unit, Input, Result>,
        feedback: Feedback<Result> = ErrorSignals(),
        input: (Request) -> Input,
    ): HttpHandler = execute(operation, { _: Request -> }, feedback, input)

    /**
     * A POST that has the actor carry a value in a presence under the key: a 204; a 404, reported as CarryRefused, if
     * the actor isn't present; a 400 if the request doesn't parse.
     */
    fun <Key : Any, Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> carry(
        presence: PresenceState<Key, Member, Primitive, Carried>,
        actor: (Request) -> Member,
        key: (Request) -> Key,
        value: (Request) -> Carried,
    ): HttpHandler = presence.carry(this, actor, key, value)

    /**
     * A POST that has the actor carry a value in a presence with no key.
     */
    fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> carry(
        presence: PresenceState<Unit, Member, Primitive, Carried>,
        actor: (Request) -> Member,
        value: (Request) -> Carried,
    ): HttpHandler = carry(presence, actor, { }, value)

    private fun known(request: Request) = lensOrNull { tenant(request) }?.let(exists) == true

    private val knownTenant = Guard({ Response(NOT_FOUND) }) { it.takeIf(::known) }
}

private val variable = Regex("\\{([^}:]+)[^}]*}")

private fun variableIn(template: String) =
    variable.find(template)?.groupValues?.get(1) ?: error("a workspace template needs a {variable} for the tenant: $template")
