package org.http4k.fanbelt.feature

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.Feedback
import org.http4k.fanbelt.input.SignalsJson
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.fanbelt.state.Tracker
import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.tenancy.Workspace
import kotlin.reflect.KProperty1

/**
 * An action declared on a feature, run in one write transaction.
 */
class Operation<Actor, Input, Result : Any> internal constructor(
    internal val name: String,
    internal val key: (Input) -> LockKey?,
    internal val about: (Input) -> Any?,
    internal val decide: (Tracker, Actor, Input) -> Outcome<Result>,
) {

    @PublishedApi
    internal fun handler(workspace: Workspace, feedback: Feedback<Result>, resets: Map<String, Any?>, parse: (Request) -> Pair<Actor, Input>): HttpHandler = { request ->
        when (val parsed = lensOrNull { parse(request) }) {
            null -> Response(BAD_REQUEST)
            else -> workspace.scope(request).execute(this, parsed.first, parsed.second).respond(request, feedback, resets)
        }
    }
}

@PublishedApi
internal inline fun <reified Signals : Any> resets(reset: Set<KProperty1<Signals, *>>): Map<String, Any?> {
    val defaults = SignalsJson.asA<Signals>("{}")
    return reset.associate { it.name to it.get(defaults) }
}

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
