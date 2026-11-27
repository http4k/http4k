package org.http4k.fanbelt.feature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import java.time.Instant
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.page.Admitted
import org.http4k.fanbelt.state.carry
import org.http4k.fanbelt.state.join
import org.http4k.fanbelt.state.read
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Test

class VisitTest {
    private class Who private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Who>(::Who)
    }

    private class Room private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Room>(::Room)
    }

    private object Hotel : Feature<Unit, Who>({}) {
        val lobby by presence(Who, carrying = Room)
        val inRoom by presence(Room, Who)

        val checksIn by visit { scope, guest, room: Room ->
            val inLobby = lobby.join(scope, guest)
            val inSuite = inRoom.join(scope, guest, room)
            lobby.carry(scope, guest, room)
            AutoCloseable { inSuite.close(); inLobby.close() }
        }

        val wandersIn by visit { scope, guest -> lobby.join(scope, guest) }
    }

    private sealed interface Caller {
        data class Guest(val who: Who) : Caller
        data object Porter : Caller
    }

    private object Inn : Feature<Unit, Caller>({}) {
        val lobby by presence(Who)
        val inRoom by presence(Room, Who)

        val guest by rule { _, caller -> if (caller is Caller.Guest) Allowed(caller) else Denied }
        val booked by rule { _, caller, room: Room -> if (caller is Caller.Guest && room != Room.of("attic")) Allowed(caller) else Hidden }

        val staysIn by visit(guest) { scope, _, room: Room, guest -> inRoom.join(scope, guest.who, room) }
        val sleepsIn by visit(booked) { scope, _, room, guest -> inRoom.join(scope, guest.who, room) }
        val strolls by visit(guest) { scope, _, guest -> lobby.join(scope, guest.who) }
    }

    private val scheduler = DeterministicScheduler(Instant.EPOCH)
    private val events = mutableListOf<Event>()
    private val scope = AppInfra(AppRuntime(scheduler = scheduler, events = { events += it })).apply { start() }.scope(TenantId.of("acme"))
    private val refusals get() = events.filterIsInstance<MetadataEvent>().map { it.event }.filterIsInstance<VisitRefused>()
    private val alex = Who.of("alex")
    private val suite = Room.of("suite")

    @Test
    fun `a visit does what arriving means, and closing it leaves`() {
        val visit = Hotel.checksIn.open(scope, alex, suite)
        val arrived = Hotel.lobby.read(scope) to Hotel.inRoom.read(scope, suite)

        visit.close()

        assertThat(arrived, equalTo(mapOf(alex to suite) to mapOf(alex to null)))
        assertThat(Hotel.lobby.read(scope) to Hotel.inRoom.read(scope, suite), equalTo(emptyMap<Who, Room?>() to emptyMap()))
    }

    @Test
    fun `a visit is a page's connect as it stands, when the page's actor and key are its own`() {
        Hotel.checksIn(scope, Admitted(alex, suite, "what the rule found")).use {
            assertThat(Hotel.inRoom.read(scope, suite).keys, equalTo(setOf(alex)))
        }
    }

    @Test
    fun `a page whose actor and key aren't the visit's binds it from who it admitted`() {
        val connect = Hotel.checksIn({ admitted: Admitted<String, Int, String> -> Who.of(admitted.actor) }) { Room.of(it.found) }

        connect(scope, Admitted("alex", 7, "suite")).use {
            assertThat(Hotel.inRoom.read(scope, suite).keys, equalTo(setOf(alex)))
        }
    }

    @Test
    fun `a visit with no input is bound with who alone`() {
        val connect = Hotel.wandersIn { admitted: Admitted<String, Unit, Unit> -> Who.of(admitted.actor) }

        connect(scope, Admitted("alex", Unit, Unit)).use {
            assertThat(Hotel.lobby.read(scope).keys, equalTo(setOf(alex)))
        }
    }

    @Test
    fun `a visit guarded by a rule is handed what it found, and opens nothing for someone it refuses`() {
        Inn.staysIn.open(scope, Caller.Guest(alex), suite).use {
            assertThat(Inn.inRoom.read(scope, suite).keys, equalTo(setOf(alex)))
        }

        Inn.staysIn.open(scope, Caller.Porter, suite).use {
            assertThat(Inn.inRoom.read(scope, suite).keys, equalTo(emptySet()))
        }
        assertThat(refusals, equalTo(listOf(VisitRefused("staysIn", Caller.Porter, null, "guest", Denied))))
    }

    @Test
    fun `a visit guarded by a rule on its input says what about when it refuses`() {
        Inn.sleepsIn.open(scope, Caller.Guest(alex), Room.of("attic")).close()

        assertThat(Inn.inRoom.read(scope, Room.of("attic")).keys, equalTo(emptySet()))
        assertThat(refusals, equalTo(listOf(VisitRefused("sleepsIn", Caller.Guest(alex), Room.of("attic"), "booked", Hidden))))
    }

    @Test
    fun `a guarded visit with no input is opened with the actor alone`() {
        Inn.strolls.open(scope, Caller.Guest(alex), Unit).use {
            assertThat(Inn.lobby.read(scope).keys, equalTo(setOf(alex)))
        }
    }
}
