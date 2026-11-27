package org.http4k.fanbelt.feature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Instant

class RuleBindingTest {
    private object Notes : Feature<Unit, String>({}) {
        private val notes = mapOf(1 to "Buy milk", 2 to "Sam's secret")

        val yourNote by rule { _, reader, id: Int ->
            when {
                id !in notes -> Hidden
                id == 2 && reader != "sam" -> Denied
                else -> Allowed(notes.getValue(id))
            }
        }

        val pinboard by rule { _, reader -> if (reader == "sam") Allowed(notes.values.joinToString()) else Denied }
    }

    private val infra = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH))).apply { start() }
    private val workspace = acmeWorkspace(infra)
    private val id = Path.int().of("id")
    private val reader = Query.required("as")

    private val app = routes(
        "/notes/{id}" bind GET to Notes.yourNote(workspace, reader, id) { note, id -> Response(OK).body("$id: $note") },
        "/pinboard" bind GET to Notes.pinboard(workspace, reader) { pinned -> Response(OK).body(pinned) },
    )

    @Test
    fun `a route bound to a rule answers with what the rule found`() {
        val response = app(Request(GET, "/notes/2?as=sam"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("2: Sam's secret"))
    }

    @Test
    fun `a route bound to a rule answers a refusal with its status`() {
        assertThat(app(Request(GET, "/notes/2?as=eve")).status, equalTo(FORBIDDEN))
        assertThat(app(Request(GET, "/notes/9?as=sam")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `a key that doesn't parse is a bad request, as it is for a page or an operation`() {
        assertThat(app(Request(GET, "/notes/two?as=sam")).status, equalTo(BAD_REQUEST))
        assertThat(app(Request(GET, "/notes/1")).status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `a rule with no key binds with the actor alone`() {
        assertThat(app(Request(GET, "/pinboard?as=sam")).bodyString(), equalTo("Buy milk, Sam's secret"))
        assertThat(app(Request(GET, "/pinboard?as=eve")).status, equalTo(FORBIDDEN))
    }
}
