/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.events.eventTenant
import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.feature.Access.Denied
import org.http4k.fanbelt.feature.Access.Hidden
import org.http4k.fanbelt.feature.Access.Unavailable
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.Table
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RulesTest {
    private data class Room(override val id: String, val members: Set<String>, val locked: Boolean = false) : Identified<String>

    private class Who private constructor(value: String) : StringValue(value) {
        companion object : StringValueFactory<Who>(::Who)
    }

    private object Rooms : Feature<Table<String, Room>, String>({ tables -> tables(TableName.of("rooms")) }) {
        val here by presence(Who)

        val enterable by rule { ctx, who, name: String ->
            val room = ctx.domain.get(name)
            when {
                room == null || who !in room.members -> Hidden
                room.locked -> Denied
                else -> Allowed(room)
            }
        }

        val present by rule { ctx, who, _: String -> if (Who.of(who) in ctx.read(here)) Allowed(who) else Hidden }

        val knock by operation(enterable, { room: String -> room }) { _, who, _, room -> Changes.of(emptySet(), "$who knocked on ${room.id}") }

        val wave by operation(present, { room: String -> room }) { _, _, _, _ -> Changes.none() }

        val create by operation { rooms, _, room: Room ->
            rooms.insert(room)
            Changes.of(emptySet(), room.id)
        }
    }

    private data class Claims(val subject: String, val scopes: Set<String>, val expires: Instant)

    private class Reports(clock: Clock) : Feature<Unit, Claims>({}) {
        val publishing by rule { ctx, claims, report: String ->
            ctx.staleAt(claims.expires)
            when {
                clock.instant() >= claims.expires -> Hidden
                "reports:write" !in claims.scopes -> Denied
                else -> Allowed(report)
            }
        }

        val publish by operation(publishing, { report: String -> report }) { _, claims, _, report -> Changes.of(emptySet(), "${claims.subject} published $report") }
    }

    private sealed interface Caller {
        data class Member(val id: String) : Caller
        data object Guest : Caller
    }

    private object Club : Feature<Table<String, Room>, Caller>({ tables -> tables(TableName.of("club")) }) {
        val member by rule { _, actor -> if (actor is Caller.Member) Allowed(actor) else Denied }

        val greet by operation(member) { _, _, name: String, member -> Changes.of(emptySet(), "${member.id} greets $name") }

        val yours by rule { ctx, actor, name: String ->
            val room = ctx.domain.get(name)
            when {
                room == null -> Unavailable(FieldName.of("room"), "That room has gone")
                actor !is Caller.Member || actor.id !in room.members -> Hidden
                else -> Allowed(room)
            }
        }

        val tidied by rule { ctx, actor, name: String ->
            ctx.read(yours, Attempt(actor, name)).then { room -> if (room.locked) Denied else Allowed(room.id) }
        }

        val tidy by operation(tidied) { _, _, _, room -> Changes.of(emptySet(), "tidied $room") }

        val create by operation { rooms, _, room: Room ->
            rooms.insert(room)
            Changes.none()
        }
    }

    private val now = Instant.parse("2026-11-07T12:00:00Z")
    private val reports = Reports(Clock.fixed(now, ZoneOffset.UTC))

    private val events = mutableListOf<Event>()

    private fun refusals() = events.filterIsInstance<MetadataEvent>().map { eventTenant(it) to it.event }.filter { it.second is OperationRefused }
    private val runtime = AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH), events = { events += it })
    private val infra = AppInfra(runtime).apply { start() }
    private val scope = infra.scope(TenantId.of("acme"))

    private fun rooms(vararg rooms: Room) = rooms.forEach { scope.execute(Rooms.create, "ann", it) }

    @Test
    fun `an operation runs with what its rule found`() {
        rooms(Room("kitchen", setOf("ann")))

        assertEquals("ann knocked on kitchen", (scope.execute(Rooms.knock, "ann", "kitchen") as Changes).result)
    }

    @Test
    fun `something you can't know about is hidden, whether it's missing or not yours`() {
        rooms(Room("kitchen", setOf("ann")))

        assertEquals(Outcome.Hidden, scope.execute(Rooms.knock, "bob", "kitchen"))
        assertEquals(Outcome.Hidden, scope.execute(Rooms.knock, "bob", "nowhere"))
    }

    @Test
    fun `something you can see but can't do is denied`() {
        rooms(Room("kitchen", setOf("ann"), locked = true))

        assertEquals(Outcome.Denied, scope.execute(Rooms.knock, "ann", "kitchen"))
    }

    @Test
    fun `a rule can be read, so a page can ask the same question`() {
        rooms(Room("kitchen", setOf("ann")))

        assertEquals(Hidden, scope.read(Rooms.enterable, Attempt("bob", "kitchen")))
        assertEquals(Allowed(Room("kitchen", setOf("ann"))), scope.read(Rooms.enterable, Attempt("ann", "kitchen")))
    }

    @Test
    fun `a rule with no key narrows the actor for the operation`() {
        assertEquals("ann greets bob", (scope.execute(Club.greet, Caller.Member("ann"), "bob") as Changes).result)
        assertEquals(Outcome.Denied, scope.execute(Club.greet, Caller.Guest, "bob"))
    }

    @Test
    fun `a rule loads what it checks, and hands it down`() {
        scope.execute(Club.create, Caller.Guest, Room("den", setOf("ann")))

        assertEquals("tidied den", (scope.execute(Club.tidy, Caller.Member("ann"), "den") as Changes).result)
        assertEquals(Outcome.Hidden, scope.execute(Club.tidy, Caller.Member("bob"), "den"))
    }

    @Test
    fun `a rule can say the thing has gone, which is a race and not a refusal`() {
        events.clear()

        assertEquals(Outcome.Rejected(FieldName.of("room"), "That room has gone"), scope.execute(Club.tidy, Caller.Member("ann"), "den"))
        assertEquals(emptyList<Pair<TenantId?, OperationRefused>>(), refusals())
    }

    @Test
    fun `a rule chains on another, stopping at the first that refuses`() {
        scope.execute(Club.create, Caller.Guest, Room("den", setOf("ann"), locked = true))

        assertEquals(Denied, scope.read(Club.tidied, Attempt(Caller.Member("ann"), "den")))
        assertEquals(Hidden, scope.read(Club.tidied, Attempt(Caller.Member("bob"), "den")))
    }

    @Test
    fun `what a rule found can be read, or nothing if it refused`() {
        assertEquals("found", Allowed("found").orNull())
        assertNull(Hidden.orNull())
    }

    @Test
    fun `an actor can be the claims from a token`() {
        val writer = Claims("ann", setOf("reports:write"), now.plusSeconds(60))

        assertEquals("ann published q3", (scope.execute(reports.publish, writer, "q3") as Changes).result)
        assertEquals(Outcome.Denied, scope.execute(reports.publish, writer.copy(scopes = emptySet()), "q3"))
    }

    @Test
    fun `a token that has expired is hidden, and a watcher re-reads the rule when it does`() {
        val claims = Claims("ann", setOf("reports:write"), now.plusSeconds(60))

        assertEquals(claims.expires, scope.reading { ctx -> ctx.read(reports.publishing, Attempt(claims, "q3")) }.staleAt)
        assertEquals(Outcome.Hidden, scope.execute(reports.publish, claims.copy(expires = now), "q3"))
    }

    @Test
    fun `a refusal says which operation and rule, carries its tenant like every event in a scope, and says nothing about who, and an allowed operation reports none`() {
        rooms(Room("kitchen", setOf("ann"), locked = true), Room("hall", setOf("ann")))
        events.clear()

        scope.execute(Rooms.knock, "bob", "kitchen")
        scope.execute(Rooms.knock, "ann", "kitchen")
        scope.execute(Rooms.knock, "ann", "hall")

        assertEquals(
            listOf(
                TenantId.of("acme") to OperationRefused("knock", "enterable", Refusal.Hidden),
                TenantId.of("acme") to OperationRefused("knock", "enterable", Refusal.Denied),
            ),
            refusals(),
        )
    }

    @Test
    fun `a rule that reads presence fails loudly in an operation rather than seeing nobody`() {
        val failure = assertThrows(IllegalStateException::class.java) { scope.execute(Rooms.wave, "ann", "kitchen") }

        assertEquals("here is presence, which an operation can't see", failure.message)
    }

    @Test
    fun `over http, hidden is a 404 and denied is a 403`() {
        rooms(Room("kitchen", setOf("ann"), locked = true))
        val knock = Rooms.knock(Workspace(infra, "/{team}") { true }, { request: Request -> request.header("who")!! }) { "kitchen" }
        val app = routes("/{team}" bind POST to knock)

        assertEquals(NOT_FOUND, app(Request(POST, "/acme").header("who", "bob")).status)
        assertEquals(FORBIDDEN, app(Request(POST, "/acme").header("who", "ann")).status)
    }
}
