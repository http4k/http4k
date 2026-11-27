package org.http4k.fanbelt.page

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import java.time.Instant
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
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

    private val n = Path.int().of("n")

    private val thing = Region("thing", renderer, n, DerivedState("thing") { _, _: Int -> "thing" }) { value, n -> Shown("$value $n") }

    private val page = Page(workspace, Layout(listOf(thing), renderer) { shell -> Shell(shell.regions.getValue("thing")) })

    private val app = poly("/things/{n}" bind page)

    @Test
    fun `one bind serves the page and its stream at the same path`() {
        val html = app.http!!(Request(GET, "/things/3"))

        assertThat(html.status, equalTo(OK))
        assertThat(html.bodyString(), containsSubstring("thing 3"))
        assertThat(app.sse!!(Request(GET, "/things/3")).status, equalTo(OK))
    }

    @Test
    fun `a post to the page's path is left to the routes beside it, even when it asks for a stream`() {
        assertThat(app.sse!!(Request(POST, "/things/3")).handled, equalTo(false))
    }

    @Test
    fun `a path value that doesn't parse is a bad request, for the page and its stream`() {
        assertThat(app.http!!(Request(GET, "/things/three")).status, equalTo(BAD_REQUEST))
        val stream = app.sse!!(Request(GET, "/things/three"))
        assertThat(stream.status, equalTo(BAD_REQUEST))
        assertThat(stream.handled, equalTo(true))
    }

    @Test
    fun `a region's view is handed whoever the page admitted`() {
        val greeting = Region("thing", renderer, n, DerivedState("greeting") { _, _: Int -> "hello" }) { value: String, who: String, n: Int -> Shown("$value $who, on $n") }
        val greeted = Page(workspace, Layout(listOf(greeting), renderer) { shell -> Shell(shell.regions.getValue("thing")) }, Viewing({ request -> request.query("who")!! }))

        assertThat(poly("/things/{n}" bind greeted).http!!(Request(GET, "/things/3?who=ann")).bodyString(), containsSubstring("hello ann, on 3"))
    }

    @Test
    fun `the shell names the tenant the page is for`() {
        val named = Page(workspace, Layout(listOf(thing), renderer) { shell -> Shell("for ${shell.tenant}") })

        assertThat(poly("/things/{n}" bind named).http!!(Request(GET, "/things/3")).bodyString(), containsSubstring("for acme"))
    }

    @Test
    fun `a layout can be handed the scope, to read once what the page shows frozen`() {
        val divider = DerivedState("divider") { _, n: Int -> "unread from $n" }
        val frozen = Page(workspace, Layout(listOf(thing), renderer) { scope, _, (_, key) -> Shell(divider.read(scope, key)) }, Viewing({ }, n))

        assertThat(poly("/things/{n}" bind frozen).http!!(Request(GET, "/things/3")).bodyString(), containsSubstring("unread from 3"))
    }
}
