package org.http4k.fanbelt.state

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.parseOrNull
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.Workspace

/**
 * Who's present, with the value each carries, if any. Joining, leaving and carrying a value publish its topic; they are not
 * operations.
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

    fun join(scope: Scope, key: Key, who: Member): Subscription =
        scope.presence.join(topic(key), member(who))

    fun carry(scope: Scope, key: Key, who: Member, value: Carried) =
        scope.presence.set(topic(key), member(who), checkNotNull(carrying).show(value))

    fun carry(
        workspace: Workspace,
        who: (Request) -> Member,
        key: (Request) -> Key,
        value: (Request) -> Carried
    ): HttpHandler = { request ->
        when (val parsed = lensOrNull { Triple(key(request), who(request), value(request)) }) {
            null -> Response(BAD_REQUEST)

            else -> {
                carry(workspace.scope(request), parsed.first, parsed.second, parsed.third)
                Response(NO_CONTENT)
            }
        }
    }

    private fun topic(key: Key) = topic(name, show(key))

    private fun member(who: Member) = PresenceMember.of(members.show(who))
}

/**
 * Binds carrying a value for a member to a POST: a 204, or a 400 if the request doesn't parse.
 */
fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> PresenceState<Unit, Member, Primitive, Carried>.carry(
    workspace: Workspace,
    who: (Request) -> Member,
    value: (Request) -> Carried
): HttpHandler =
    carry(workspace, who, { }, value)

/**
 * Joins a presence state with no key; close the lease to leave.
 */
fun <Member : Value<Primitive>, Primitive : Any> PresenceState<Unit, Member, Primitive, *>.join(
    scope: Scope,
    who: Member
): Subscription = join(scope, Unit, who)

/**
 * Carries a value for a member in a presence state with no key.
 */
fun <Member : Value<Primitive>, Primitive : Any, Carried : Value<*>> PresenceState<Unit, Member, Primitive, Carried>.carry(
    scope: Scope,
    who: Member,
    value: Carried
) = carry(scope, Unit, who, value)
