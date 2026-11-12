/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.config.Environment
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.state.DerivedState
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class KeepAliveTest {
    private data class Shown(val value: String) : ViewModel

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T00:00:00Z"))
    private val infra = AppInfra(AppRuntime(scheduler = scheduler)).apply { start() }
    private val workspace = acmeWorkspace(infra)
    private val state = DerivedState<Unit, String>("shown") { ctx, _ ->
        ctx.watch(Topic.of("t"))
        "v1"
    }
    private val region = Region("r", { """<p id="r">${(it as Shown).value}</p>""" }, state) { model, _ -> Shown(model) }
    private val page = Page(workspace, Layout(listOf(region), { "" }) { _, _ -> Shown("") })

    private val here = Topic.of("here")
    private val ann = PresenceMember.of("ann")

    private fun present() = workspace.scope(Request(GET, "/")).presence.present(mapOf(here to Unit::class)).getValue(here).keys

    private fun connected(to: Page = page, dead: Boolean = false) = to.connect(Request(GET, "/"), dead)

    @Test
    fun `a quiet stream is pinged every two seconds`() {
        val stream = connected()

        scheduler.tick(Duration.ofSeconds(6))

        assertEquals(3, stream.pings)
    }

    @Test
    fun `a node's settings choose how often its streams are pinged`() {
        val every10Seconds = AppInfra(AppRuntime(scheduler = scheduler), env = Environment.from("FANBELT_STREAM_KEEPALIVE" to "PT10S")).apply { start() }
        val page = Page(acmeWorkspace(every10Seconds), Layout(listOf(region), { "" }) { _, _ -> Shown("") })
        val stream = connected(page)

        scheduler.tick(Duration.ofSeconds(20))

        assertEquals(2, stream.pings)
    }

    @Test
    fun `a ping that can't be written closes the stream and ends the viewer's presence, and it stays ended`() {
        val joining = Page(workspace, Layout(listOf(region), { "" }) { _, _ -> Shown("") }, Viewing { scope, _ -> scope.presence.join(here, ann) })
        val stream = connected(joining, dead = true)
        assertEquals(setOf(ann), present())

        scheduler.tick(Duration.ofSeconds(2))
        assertTrue(stream.closed)
        assertEquals(emptySet<PresenceMember>(), present())

        scheduler.tick(Duration.ofSeconds(40))
        assertEquals(emptySet<PresenceMember>(), present())
    }

    @Test
    fun `a closed stream is pinged no more`() {
        val stream = connected()
        scheduler.tick(Duration.ofSeconds(2))

        stream.close()
        scheduler.tick(Duration.ofSeconds(6))

        assertEquals(1, stream.pings)
    }
}
