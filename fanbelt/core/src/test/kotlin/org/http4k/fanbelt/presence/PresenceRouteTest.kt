package org.http4k.fanbelt.presence


import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.carry
import org.http4k.fanbelt.state.join
import org.http4k.fanbelt.state.read
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.lens.Query
import org.http4k.lens.value
import org.junit.jupiter.api.Test

class PresenceRouteTest {
    private class Who private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Who>(::Who)
    }

    private class Cell private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Cell>(::Cell)
    }

    private object Sheet : Feature<Unit, Unit>({}) {
        val cursor by presence(Who, carrying = Cell)
    }

    private val cell = Query.value(Cell).required("cell")
    private val events = CopyOnWriteArrayList<Event>()
    private val infra = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH), events = { events += if (it is MetadataEvent) it.event else it })).apply { start() }
    private val workspace = acmeWorkspace(infra)
    private val scope = infra.scope(TenantId.of("acme"))
    private val alex = Who.of("alex")

    @Test
    fun `a presence value binds to a route like an operation does, naming its actor the same way, and answers 204`() {
        Sheet.cursor.join(scope, actor = alex)
        val route = Sheet.cursor.carry(workspace, actor = { alex }) { cell(it) }

        val response = route(Request(POST, "/cursor").query("cell", "B2"))

        assertThat(response.status, equalTo(NO_CONTENT))
        assertThat(Sheet.cursor.read(scope)[alex], equalTo(Cell.of("B2")))
    }

    @Test
    fun `a value that doesn't parse is a bad request`() {
        Sheet.cursor.join(scope, alex)
        val route = Sheet.cursor.carry(workspace, { alex }) { cell(it) }

        assertThat(route(Request(POST, "/cursor")).status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `someone carrying a value where they aren't present is refused, changes nothing, and is on the record`() {
        val route = Sheet.cursor.carry(workspace, { alex }) { cell(it) }

        val response = route(Request(POST, "/cursor").query("cell", "B2"))

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(Sheet.cursor.read(scope), equalTo(emptyMap()))
        assertThat(events.filterIsInstance<CarryRefused>(), equalTo(listOf(CarryRefused("cursor", alex, null))))
    }
}
