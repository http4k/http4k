/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.sse.SseMessage
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class FirstConnectTest {
    private data class Shown(val id: String, val value: String) : ViewModel
    private data class Shell(val signals: String) : ViewModel

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T00:00:00Z"))
    private val infra = AppInfra(AppRuntime(scheduler = scheduler)).apply { start() }
    private val topic = Topic.of("t")
    private val values = ConcurrentHashMap(mapOf("a" to "a1", "b" to "b1"))
    private val workspace = acmeWorkspace(infra)

    private fun region(id: String) = Region(id, { """<p id="${(it as Shown).id}">${it.value}</p>""" }, DerivedState<Unit, String>(id) { ctx, _ ->
        ctx.watch(topic)
        values.getValue(id)
    }) { value, _ -> Shown(id, value) }

    private val page = Page(workspace, Layout(listOf(region("a"), region("b")), { (it as Shell).signals }) { _, shell -> Shell(shell.signals) })

    private fun paintedSignals() = page.http(Request(GET, "/")).bodyString()

    private fun connect(signals: String?): List<SseMessage.Event> {
        val stream = page.connect(if (signals == null) Request(GET, "/") else Request(GET, "/").query("datastar", signals))
        scheduler.runUntilIdle()
        return stream.events
    }

    private fun List<SseMessage.Event>.patched() = filter { it.event == "datastar-patch-elements" }.map { it.data.substringAfter("elements ").trim() }

    private fun List<SseMessage.Event>.cleared() = any { it.event == "datastar-patch-signals" && it.data.contains("\"fanbeltPainted\":null") }

    @Test
    fun `a first connect after the page sends only what changed since it was painted, and clears what the page knew`() {
        val signals = paintedSignals()
        values["b"] = "b2"

        val received = connect(signals)

        assertEquals(listOf("""<p id="b">b2</p>"""), received.patched())
        assertEquals(true, received.cleared())
    }

    @Test
    fun `a connect without the page's digests, such as a reconnect, gets every region`() {
        val received = connect(null)

        assertEquals(listOf("""<p id="a">a1</p>""", """<p id="b">b1</p>"""), received.patched())
        assertEquals(false, received.cleared())
    }

    @Test
    fun `digests that don't parse are ignored`() {
        assertEquals(2, connect("not json").patched().size)
    }
}
