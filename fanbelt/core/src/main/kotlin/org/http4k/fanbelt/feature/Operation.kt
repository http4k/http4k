package org.http4k.fanbelt.feature

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.ErrorSignals
import org.http4k.fanbelt.input.Feedback
import org.http4k.fanbelt.input.SignalsJson
import org.http4k.fanbelt.input.SignalsJson.datastarModel
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.fanbelt.state.Tracker
import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.tenancy.Workspace
import kotlin.reflect.KProperty1

/**
 * An action declared with operation { }. Invoked with a Workspace it becomes a POST route: input that doesn't parse is
 * a 400, Denied a 403, Hidden a 404, and accepted or rejected outcomes go to the Feedback.
 */
class Operation<Actor, Input, Result : Any> internal constructor(
    internal val name: String,
    internal val key: (Input) -> LockKey?,
    internal val decide: (Tracker, Actor, Input) -> Outcome<Result>,
) {

    /**
     * Binds the operation to a POST, building its input from the request's signals and the key the request gives, such
     * as a path value. Signals named in reset go back to their defaults once it's accepted.
     */
    inline operator fun <reified Signals : Any, Key> invoke(
        workspace: Workspace,
        noinline actor: (Request) -> Actor,
        noinline key: (Request) -> Key,
        reset: Set<KProperty1<Signals, *>> = emptySet(),
        feedback: Feedback<Result> = ErrorSignals(),
        crossinline input: (Signals, Key) -> Input,
    ): HttpHandler = handler(workspace, feedback, resets(reset)) { request -> actor(request) to input(request.datastarModel<Signals>(), key(request)) }

    /**
     * Binds the operation to a POST, building its input from the request's signals alone. Signals named in reset go
     * back to their defaults once it's accepted.
     */
    inline operator fun <reified Signals : Any> invoke(
        workspace: Workspace,
        noinline actor: (Request) -> Actor,
        reset: Set<KProperty1<Signals, *>> = emptySet(),
        feedback: Feedback<Result> = ErrorSignals(),
        crossinline input: (Signals) -> Input,
    ): HttpHandler = handler(workspace, feedback, resets(reset)) { request -> actor(request) to input(request.datastarModel<Signals>()) }

    /**
     * Binds the operation to a POST, taking its input from the request.
     */
    operator fun invoke(
        workspace: Workspace,
        actor: (Request) -> Actor,
        feedback: Feedback<Result> = ErrorSignals(),
        input: (Request) -> Input,
    ): HttpHandler = handler(workspace, feedback, emptyMap()) { request -> actor(request) to input(request) }

    @PublishedApi
    internal fun handler(workspace: Workspace, feedback: Feedback<Result>, resets: Map<String, Any?>, parse: (Request) -> Pair<Actor, Input>): HttpHandler = { request ->
        when (val parsed = lensOrNull { parse(request) }) {
            null -> Response(BAD_REQUEST)
            else -> workspace.scope(request).execute(this, parsed.first, parsed.second).respond(request, feedback, resets)
        }
    }
}

/**
 * Binds an operation with no actor to a POST, building its input from the request's signals and the key it gives.
 */
inline operator fun <reified Signals : Any, Key, Input, Result : Any> Operation<Unit, Input, Result>.invoke(
    workspace: Workspace,
    noinline key: (Request) -> Key,
    reset: Set<KProperty1<Signals, *>> = emptySet(),
    feedback: Feedback<Result> = ErrorSignals(),
    crossinline input: (Signals, Key) -> Input,
): HttpHandler = invoke(workspace, { _: Request -> }, key, reset, feedback, input)

/**
 * Binds an operation with no actor to a POST, building its input from the request's signals alone.
 */
inline operator fun <reified Signals : Any, Input, Result : Any> Operation<Unit, Input, Result>.invoke(
    workspace: Workspace,
    reset: Set<KProperty1<Signals, *>> = emptySet(),
    feedback: Feedback<Result> = ErrorSignals(),
    crossinline input: (Signals) -> Input,
): HttpHandler = invoke(workspace, { _: Request -> }, reset, feedback, input)

@PublishedApi
internal inline fun <reified Signals : Any> resets(reset: Set<KProperty1<Signals, *>>): Map<String, Any?> {
    val defaults = SignalsJson.asA<Signals>("{}")
    return reset.associate { it.name to it.get(defaults) }
}

/**
 * Binds an operation with no actor to a POST.
 */
operator fun <Input, Result : Any> Operation<Unit, Input, Result>.invoke(
    workspace: Workspace,
    feedback: Feedback<Result> = ErrorSignals(),
    input: (Request) -> Input,
): HttpHandler = invoke(workspace, { _: Request -> }, feedback, input)

@PublishedApi
internal fun <Result : Any> Outcome<Result>.respond(request: Request, feedback: Feedback<Result>, reset: Map<String, Any?>): Response = when (this) {
    is Changes -> feedback.accepted(request, result, reset)
    is Rejected -> feedback.rejected(request, this)
    Denied -> Response(Denied.status)
    Hidden -> Response(Hidden.status)
}

internal fun <Actor : Any, Key : Any, Found> Rule<Actor, Key, Found>.found(tracker: Tracker, actorKey: ActorKey<Actor, Key>): Found =
    when (val access = read(tracker, actorKey)) {
        is Allowed -> access.found
        is Rejected -> throw Refused(access)
        Denied -> throw Refused(Denied, name.value, actorKey.key.takeIf { it != Unit })
        Hidden -> throw Refused(Hidden, name.value, actorKey.key.takeIf { it != Unit })
    }

internal class Refused(val outcome: Outcome<Nothing>, val rule: String? = null, val key: Any? = null) :
    RuntimeException(null, null, false, false)
