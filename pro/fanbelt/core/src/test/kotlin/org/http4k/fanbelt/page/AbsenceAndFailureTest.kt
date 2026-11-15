/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.RecordingSse
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class AbsenceAndFailureTest {
    private data class Shown(val text: String) : ViewModel

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T00:00:00Z"))
    private val events = CopyOnWriteArrayList<Event>()
    private val infra = AppInfra(AppRuntime(scheduler = scheduler, events = { events += if (it is MetadataEvent) it.event else it })).apply { start() }
    private val topic = Topic.of("ticket")
    private val tickets = ConcurrentHashMap(mapOf("t1" to "Cannot log in"))
    private val broken = AtomicBoolean(false)
    private val workspace = acmeWorkspace(infra)

    private val ticket = DerivedState<String, String?>("ticket") { ctx, id ->
        ctx.watch(topic)
        check(!broken.get()) { "storage is down" }
        tickets[id]
    }
    private val header = Region("header", { """<h1 id="header">${(it as Shown).text}</h1>""" }, { request: Request -> request.query("t")!! }, ticket) { subject, _ ->
        Shown(subject ?: "This ticket no longer exists")
    }
    private val clock = DerivedState<Unit, String>("clock") { ctx, _ -> ctx.watch(topic); "tick ${tickets.size}" }
    private val footer = Region("footer", { """<p id="footer">${(it as Shown).text}</p>""" }, clock) { time, _ -> Shown(time) }
    private val page = Page(workspace, Layout(listOf(header, footer), { (it as Shown).text }, subject = header) { _, shell ->
        Shown(shell.regions.values.joinToString(""))
    })

    private fun change() {
        workspace.scope(Request(GET, "/")).bus.publish(topic)
        scheduler.runUntilIdle()
    }

    private fun connect(ticket: String): RecordingSse {
        val stream = page.connect(Request(GET, "/?t=$ticket"))
        scheduler.runUntilIdle()
        return stream
    }

    @Test
    fun `a page whose subject is absent renders as usual, with its absent view, and answers 404`() {
        val found = page.http(Request(GET, "/?t=t1"))
        val missing = page.http(Request(GET, "/?t=t9"))

        assertThat(found.status, equalTo(OK))
        assertThat(missing.status, equalTo(NOT_FOUND))
        assertThat(missing.bodyString(), containsSubstring("This ticket no longer exists"))
    }

    @Test
    fun `a subject that goes absent while live shows its absent view, like any other change`() {
        val stream = connect("t1")

        tickets.remove("t1")
        change()

        assertThat(stream.received.last { it.contains("header") }, containsSubstring("This ticket no longer exists"))
        assertThat(stream.closed, equalTo(false))
    }

    @Test
    fun `a region that fails to read shows its fallback and is reported, and the rest of the page stays live`() {
        val stream = connect("t1")

        broken.set(true)
        tickets["t2"] = "Printer on fire"
        change()

        assertThat(stream.closed, equalTo(false))
        assertThat(stream.received, anyElement(containsSubstring("""<div id="header"></div>""")))
        assertThat(stream.received.last { it.contains("footer") }, containsSubstring("tick 2"))
        assertThat(events.filterIsInstance<RedrawFailed>().size, equalTo(1))
    }

    @Test
    fun `a failed region heals on the next change once it reads again`() {
        val stream = connect("t1")

        broken.set(true)
        change()
        broken.set(false)
        change()

        assertThat(stream.received.last { it.contains("header") }, containsSubstring("Cannot log in"))
    }

    @Test
    fun `a region's own fallback view replaces the built-in one`() {
        val apologetic = Region("header", { """<h1 id="header">${(it as Shown).text}</h1>""" }, { request: Request -> request.query("t")!! }, ticket, Shown("Tickets are resting")) { subject, _ ->
            Shown(subject ?: "This ticket no longer exists")
        }
        val page = Page(workspace, Layout(listOf(apologetic), { (it as Shown).text }) { _, shell -> Shown(shell.regions.getValue("header")) })
        broken.set(true)

        assertThat(page.http(Request(GET, "/?t=t1")).bodyString(), equalTo("""<h1 id="header">Tickets are resting</h1>"""))
    }

    @Test
    fun `a region that fails on first paint shows its fallback, and the page still renders`() {
        broken.set(true)

        val response = page.http(Request(GET, "/?t=t1"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("""<div id="header"></div>"""))
        assertThat(response.bodyString(), containsSubstring("tick 1"))
        assertThat(events.filterIsInstance<RedrawFailed>().size, equalTo(1))
    }
}
