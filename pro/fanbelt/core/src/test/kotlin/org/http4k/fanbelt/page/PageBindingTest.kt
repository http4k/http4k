/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

        assertEquals(OK, html.status)
        assertTrue(html.bodyString().contains("thing 3"))
        assertEquals(OK, app.sse!!(Request(GET, "/things/3")).status)
    }

    @Test
    fun `a path value that doesn't parse is a bad request, for the page and its stream`() {
        assertEquals(BAD_REQUEST, app.http!!(Request(GET, "/things/three")).status)
        val stream = app.sse!!(Request(GET, "/things/three"))
        assertEquals(BAD_REQUEST, stream.status)
        assertTrue(stream.handled)
    }
}
