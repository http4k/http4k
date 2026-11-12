/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
import kotlin.reflect.KClass

class PresenceState<Key : Any, Member : Value<Primitive>, Primitive : Any, Carried : Any> internal constructor(
    override val name: StateName,
    private val show: (Key) -> String,
    private val members: ValueFactory<Member, Primitive>,
    private val carrying: KClass<Carried>,
) : State<Key, Map<Member, Carried?>>() {

    override fun read(tracker: Tracker, key: Key): Map<Member, Carried?> {
        val topic = topic(key)
        tracker.watch(topic)
        return tracker.present(
            topic,
            carrying
        ).entries.mapNotNull { (member, carried) ->
            members.parseOrNull(member.value)?.to(carried?.let(carrying.javaObjectType::cast))
        }.toMap()
    }

    fun join(scope: Scope, key: Key, who: Member): Subscription =
        scope.presence.join(topic(key), member(who))

    fun set(scope: Scope, key: Key, who: Member, value: Carried) =
        scope.presence.set(topic(key), member(who), value)

    fun set(
        workspace: Workspace,
        who: (Request) -> Member,
        key: (Request) -> Key,
        value: (Request) -> Carried
    ): HttpHandler = { request ->
        when (val parsed = lensOrNull { Triple(key(request), who(request), value(request)) }) {
            null -> Response(BAD_REQUEST)

            else -> {
                set(workspace.scope(request), parsed.first, parsed.second, parsed.third)
                Response(NO_CONTENT)
            }
        }
    }

    private fun topic(key: Key) = topic(name, show(key))

    private fun member(who: Member) = PresenceMember.of(members.show(who))
}

fun <Member : Value<Primitive>, Primitive : Any, Carried : Any> PresenceState<Unit, Member, Primitive, Carried>.set(
    workspace: Workspace,
    who: (Request) -> Member,
    value: (Request) -> Carried
): HttpHandler =
    set(workspace, who, { }, value)

fun <Member : Value<Primitive>, Primitive : Any> PresenceState<Unit, Member, Primitive, *>.join(
    scope: Scope,
    who: Member
): Subscription = join(scope, Unit, who)

fun <Member : Value<Primitive>, Primitive : Any, Carried : Any> PresenceState<Unit, Member, Primitive, Carried>.set(
    scope: Scope,
    who: Member,
    value: Carried
) = set(scope, Unit, who, value)
