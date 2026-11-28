package org.http4k.fanbelt.state

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.parseOrNull
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.fanbelt.presence.CarryRefused
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.Workspace

/**
 * Who's present, with the value each carries.
 */
class PresenceState<Key : Any, Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> internal constructor(
    override val name: StateName,
    private val show: (Key) -> String,
    private val members: ValueFactory<Member, Primitive>,
    private val carrying: ValueFactory<Carried, *>?,
) : State<Key, Map<Member, Carried?>>() {

    override fun read(tracker: Tracker, key: Key): Map<Member, Carried?> {
        val topic = topic(key)
        tracker.watch(topic)
        return tracker.present(topic).entries.mapNotNull { (member, carried) ->
            members.parseOrNull(member.value)?.to(carried?.let { carrying?.parse(it) })
        }.toMap()
    }

    internal fun join(scope: Scope, actor: Member, key: Key): Subscription =
        scope.presence.join(topic(key), member(actor))

    internal fun carry(scope: Scope, actor: Member, key: Key, value: Carried) =
        scope.presence.set(topic(key), member(actor), checkNotNull(carrying).show(value))

    internal fun carry(
        workspace: Workspace,
        actor: (Request) -> Member,
        key: (Request) -> Key,
        value: (Request) -> Carried
    ): HttpHandler = { request ->
        when (val parsed = lensOrNull { Triple(actor(request), key(request), value(request)) }) {
            null -> Response(BAD_REQUEST)

            else -> {
                val (actor, key, value) = parsed
                val scope = workspace.scope(request)
                if (scope.presence.present(setOf(topic(key)))[topic(key)].orEmpty().containsKey(member(actor))) {
                    carry(scope, actor, key, value)
                    Response(NO_CONTENT)
                } else {
                    scope.events(CarryRefused(name.value, actor, key.takeIf { it != Unit }))
                    Response(NOT_FOUND)
                }
            }
        }
    }

    private fun topic(key: Key) = topic(name, show(key))

    private fun member(actor: Member) = PresenceMember.of(members.show(actor))
}
