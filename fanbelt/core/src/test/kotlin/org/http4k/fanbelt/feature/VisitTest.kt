package org.http4k.fanbelt.feature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import java.time.Instant
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.page.Admitted
import org.http4k.fanbelt.state.carry
import org.http4k.fanbelt.state.join
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

    private val scheduler = DeterministicScheduler(Instant.EPOCH)
    private val scope = AppInfra(AppRuntime(scheduler = scheduler)).apply { start() }.scope(TenantId.of("acme"))
    private val alex = Who.of("alex")
    private val suite = Room.of("suite")

    @Test
    fun `a visit does what arriving means, and closing it leaves`() {
        val visit = Hotel.checksIn.open(scope, alex, suite)
        val arrived = scope.read(Hotel.lobby) to scope.read(Hotel.inRoom, suite)

        visit.close()

        assertThat(arrived, equalTo(mapOf(alex to suite) to mapOf(alex to null)))
        assertThat(scope.read(Hotel.lobby) to scope.read(Hotel.inRoom, suite), equalTo(emptyMap<Who, Room?>() to emptyMap()))
    }

    @Test
    fun `a visit is a page's connect as it stands, when the page's actor and key are its own`() {
        Hotel.checksIn(scope, Admitted(alex, suite, "what the rule found")).use {
            assertThat(scope.read(Hotel.inRoom, suite).keys, equalTo(setOf(alex)))
        }
    }

    @Test
    fun `a page whose actor and key aren't the visit's binds it from who it admitted`() {
        val connect = Hotel.checksIn({ admitted: Admitted<String, Int, String> -> Who.of(admitted.actor) }) { Room.of(it.found) }

        connect(scope, Admitted("alex", 7, "suite")).use {
            assertThat(scope.read(Hotel.inRoom, suite).keys, equalTo(setOf(alex)))
        }
    }

    @Test
    fun `a visit with no input is bound with who alone`() {
        val connect = Hotel.wandersIn { admitted: Admitted<String, Unit, Unit> -> Who.of(admitted.actor) }

        connect(scope, Admitted("alex", Unit, Unit)).use {
            assertThat(scope.read(Hotel.lobby).keys, equalTo(setOf(alex)))
        }
    }
}
