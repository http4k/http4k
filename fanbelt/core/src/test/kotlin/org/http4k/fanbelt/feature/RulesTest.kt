package org.http4k.fanbelt.feature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
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
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.RowId
import org.http4k.fanbelt.store.Table
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RulesTest {
    private data class Room(override val id: RowId, val members: Set<String>, val locked: Boolean = false) : Identified<RowId>

    private class Who private constructor(value: String) : StringValue(value) {
        companion object : StringValueFactory<Who>(::Who)
    }

    private object Rooms : Feature<Table<RowId, Room>, String>({ tables -> tables(TableName.of("rooms")) }) {
        val here by presence(Who)

        val enterable by rule { ctx, who, name: String ->
            val room = ctx.domain.get(RowId.of(name))
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
            Changes.of(emptySet(), room.id.value)
        }
    }

    private data class Claims(val subject: String, val scopes: Set<String>, val expires: Instant)

    private class Reports(clock: Clock) : Feature<Unit, Claims>({}) {
        val publishing by rule { ctx, claims, report: String ->
            ctx.stale(at = claims.expires)
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

    private object Club : Feature<Table<RowId, Room>, Caller>({ tables -> tables(TableName.of("club")) }) {
        val member by rule { _, actor -> if (actor is Caller.Member) Allowed(actor) else Denied }

        val greet by operation(member) { _, _, name: String, member -> Changes.of(emptySet(), "${member.id} greets $name") }

        val leave by operation(member) { _, _, member -> Changes.of(emptySet(), "${member.id} left") }

        val yours by rule { ctx, actor, name: String ->
            val room = ctx.domain.get(RowId.of(name))
            when {
                room == null -> Rejected(FieldName.of("room"), "That room has gone")
                actor !is Caller.Member || actor.id !in room.members -> Hidden
                else -> Allowed(room)
            }
        }

        val tidied by rule { ctx, actor, name: String ->
            ctx.read(yours, ActorKey(actor, name)).flatMap { room -> if (room.locked) Denied else Allowed(room.id) }
        }

        val tidy by operation(tidied) { _, _, _, room -> Changes.of(emptySet(), "tidied $room") }

        val create by operation { rooms, _, room: Room ->
            rooms.insert(room)
            Changes.none()
        }
    }

    private val now = Instant.parse("2026-11-07T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val reports = Reports(clock)

    private val events = mutableListOf<Event>()

    private fun refusals() = events.filterIsInstance<MetadataEvent>().map { eventTenant(it) to it.event }.filter { it.second is OperationRefused }
    private val runtime = AppRuntime(clock = clock, scheduler = DeterministicScheduler(now), events = { events += it })
    private val infra = AppInfra(runtime).apply { start() }
    private val scope = infra.scope(TenantId.of("acme"))

    private fun rooms(vararg rooms: Room) = rooms.forEach { scope.execute(Rooms.create, "ann", it) }

    @Test
    fun `an operation runs with what its rule found`() {
        rooms(Room(RowId.of("kitchen"), setOf("ann")))

        assertThat((scope.execute(Rooms.knock, "ann", "kitchen") as Changes).result, equalTo("ann knocked on kitchen"))
    }

    @Test
    fun `something you can't know about is hidden, whether it's missing or not yours`() {
        rooms(Room(RowId.of("kitchen"), setOf("ann")))

        assertThat(scope.execute(Rooms.knock, "bob", "kitchen"), equalTo(Outcome.Hidden))
        assertThat(scope.execute(Rooms.knock, "bob", "nowhere"), equalTo(Outcome.Hidden))
    }

    @Test
    fun `something you can see but can't do is denied`() {
        rooms(Room(RowId.of("kitchen"), setOf("ann"), locked = true))

        assertThat(scope.execute(Rooms.knock, "ann", "kitchen"), equalTo(Outcome.Denied))
    }

    @Test
    fun `a rule can be read, so a page can ask the same question`() {
        rooms(Room(RowId.of("kitchen"), setOf("ann")))

        assertThat(scope.read(Rooms.enterable, ActorKey("bob", "kitchen")), equalTo(Hidden))
        assertThat(scope.read(Rooms.enterable, ActorKey("ann", "kitchen")), equalTo(Allowed(Room(RowId.of("kitchen"), setOf("ann")))))
    }

    @Test
    fun `a rule with no key narrows the actor for the operation`() {
        assertThat((scope.execute(Club.greet, Caller.Member("ann"), "bob") as Changes).result, equalTo("ann greets bob"))
        assertThat(scope.execute(Club.greet, Caller.Guest, "bob"), equalTo(Outcome.Denied))
    }

    @Test
    fun `an operation with no input can still be guarded by a rule on the actor`() {
        assertThat((scope.execute(Club.leave, Caller.Member("ann"), Unit) as Changes).result, equalTo("ann left"))
        assertThat(scope.execute(Club.leave, Caller.Guest, Unit), equalTo(Outcome.Denied))
    }

    @Test
    fun `a rule loads what it checks, and hands it down`() {
        scope.execute(Club.create, Caller.Guest, Room(RowId.of("den"), setOf("ann")))

        assertThat((scope.execute(Club.tidy, Caller.Member("ann"), "den") as Changes).result, equalTo("tidied den"))
        assertThat(scope.execute(Club.tidy, Caller.Member("bob"), "den"), equalTo(Outcome.Hidden))
    }

    @Test
    fun `a rule can say the thing has gone, which is a race and not a refusal`() {
        events.clear()

        assertThat(scope.execute(Club.tidy, Caller.Member("ann"), "den"), equalTo(Outcome.Rejected(FieldName.of("room"), "That room has gone")))
        assertThat(refusals(), equalTo(emptyList<Pair<TenantId?, OperationRefused>>()))
    }

    @Test
    fun `a rule chains on another, stopping at the first that refuses`() {
        scope.execute(Club.create, Caller.Guest, Room(RowId.of("den"), setOf("ann"), locked = true))

        assertThat(scope.read(Club.tidied, ActorKey(Caller.Member("ann"), "den")), equalTo(Denied))
        assertThat(scope.read(Club.tidied, ActorKey(Caller.Member("bob"), "den")), equalTo(Hidden))
    }

    @Test
    fun `a rule knows its tenant, to ask a service outside the app, whether read or guarding an operation`() {
        val agents = mapOf(TenantId.of("acme") to setOf("ann"), TenantId.of("globex") to setOf("bob"))
        val desk = object : Feature<Unit, String>({}) {
            val agent by rule { ctx, who -> if (who in agents[ctx.tenant].orEmpty()) Allowed(who) else Denied }

            val answer by operation(agent) { _, _, ticket: String, who -> Changes.of(emptySet(), "$who answered $ticket") }
        }
        val globex = infra.scope(TenantId.of("globex"))

        assertThat(scope.read(desk.agent, ActorKey("ann", Unit)), equalTo(Allowed("ann")))
        assertThat(globex.read(desk.agent, ActorKey("ann", Unit)), equalTo(Denied))
        assertThat((globex.execute(desk.answer, "bob", "t1") as Changes).result, equalTo("bob answered t1"))
        assertThat(scope.execute(desk.answer, "bob", "t1"), equalTo(Outcome.Denied))
    }

    @Test
    fun `a token that has expired is hidden, and a watcher re-reads the rule when it does`() {
        val claims = Claims("ann", setOf("reports:write"), now.plusSeconds(60))

        assertThat(scope.reading { ctx -> ctx.read(reports.publishing, ActorKey(claims, "q3")) }.staleAt, equalTo(claims.expires))
        assertThat(scope.execute(reports.publish, claims.copy(expires = now), "q3"), equalTo(Outcome.Hidden))
    }

    @Test
    fun `a refusal says which operation and rule, carries its tenant like every event in a scope, and says nothing about who, and an allowed operation reports none`() {
        rooms(Room(RowId.of("kitchen"), setOf("ann"), locked = true), Room(RowId.of("hall"), setOf("ann")))
        events.clear()

        scope.execute(Rooms.knock, "bob", "kitchen")
        scope.execute(Rooms.knock, "ann", "kitchen")
        scope.execute(Rooms.knock, "ann", "hall")

        assertThat(refusals(), equalTo(listOf(
            TenantId.of("acme") to OperationRefused("knock", "enterable", Outcome.Hidden),
            TenantId.of("acme") to OperationRefused("knock", "enterable", Outcome.Denied),
        )))
    }

    @Test
    fun `a rule that reads presence fails loudly in an operation rather than seeing nobody`() {
        assertThat({ scope.execute(Rooms.wave, "ann", "kitchen") }, throws<IllegalStateException>(has(Throwable::message, equalTo("here is presence, which an operation can't see"))))
    }

    @Test
    fun `over http, hidden is a 404 and denied is a 403`() {
        rooms(Room(RowId.of("kitchen"), setOf("ann"), locked = true))
        val knock = Rooms.knock(Workspace(infra, "/{team}") { true }, { request: Request -> request.header("who")!! }) { "kitchen" }
        val app = routes("/{team}" bind POST to knock)

        assertThat(app(Request(POST, "/acme").header("who", "bob")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(POST, "/acme").header("who", "ann")).status, equalTo(FORBIDDEN))
    }
}
