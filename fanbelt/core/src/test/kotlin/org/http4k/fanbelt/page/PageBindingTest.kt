package org.http4k.fanbelt.page

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.routing.poly
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test
import java.time.Instant

class PageBindingTest {
    private data class Shown(val value: String) : ViewModel
    private data class Shell(val region: String) : ViewModel

    private val workspace = acmeWorkspace(AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH))).apply { start() })

    private val renderer = { view: ViewModel ->
        when (view) {
            is Shown -> """<p id="thing">${view.value}</p>"""
            is Shell -> "<html><body>${view.region}</body></html>"
            else -> error("unexpected $view")
        }
    }

    private val thing = Region("thing", renderer, Path.int().of("n"), DerivedState("thing") { _, n: Int -> "thing $n" }) { value, _ -> Shown(value) }

    private val page = Page(workspace, Layout(listOf(thing), renderer) { _, shell -> Shell(shell.regions.getValue("thing")) })

    private val app = poly("/things/{n}" bind page)

    @Test
    fun `one bind serves the page and its stream at the same path`() {
        val html = app.http!!(Request(GET, "/things/3"))

        assertThat(html.status, equalTo(OK))
        assertThat(html.bodyString(), containsSubstring("thing 3"))
        assertThat(app.sse!!(Request(GET, "/things/3")).status, equalTo(OK))
    }

    @Test
    fun `a path value that doesn't parse is a bad request, for the page and its stream`() {
        assertThat(app.http!!(Request(GET, "/things/three")).status, equalTo(BAD_REQUEST))
        val stream = app.sse!!(Request(GET, "/things/three"))
        assertThat(stream.status, equalTo(BAD_REQUEST))
        assertThat(stream.handled, equalTo(true))
    }
}
