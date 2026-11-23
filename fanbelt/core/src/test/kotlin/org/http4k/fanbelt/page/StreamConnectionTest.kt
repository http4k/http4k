/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.greaterThanOrEqualTo
import com.natpryce.hamkrest.lessThan
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
import org.http4k.sse.SseMessage
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger

class StreamConnectionTest {
    private data class Shown(val value: String) : ViewModel

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T00:00:00Z"))
    private val infra = AppInfra(AppRuntime(scheduler = scheduler, random = Random(0))).apply { start() }
    private val workspace = acmeWorkspace(infra)
    private val state = DerivedState<Unit, String>("shown") { ctx, _ ->
        ctx.watch(Topic.of("t"))
        "v1"
    }
    private val region = Region("r", { """<p id="r">${(it as Shown).value}</p>""" }, state) { model, _ -> Shown(model) }
    private val page = Page(workspace, Layout(listOf(region), { "" }) { _, _ -> Shown("") })

    private val here = Topic.of("here")
    private val ann = PresenceMember.of("ann")

    private fun present() = workspace.scope(Request(GET, "/")).presence.present(setOf(here)).getValue(here).keys

    private fun connected(to: Page = page, dead: Boolean = false) = to.connect(Request(GET, "/"), dead)

    @Test
    fun `the page's connect action closes while the page is hidden, so a page left in the back-forward cache gives its connection back, and keeps retrying after clean closes and gateway errors`() {
        val page = Page(workspace, Layout(listOf(region), { (it as Shown).value }) { _, shell -> Shown(shell.connect) })

        assertThat(page.http(Request(GET, "/live")).bodyString(), equalTo("@get('/live', {retry: 'always', retryMaxCount: 1000})"))
    }

    @Test
    fun `every stream starts by telling the browser a jittered retry delay, so a node's clients don't reconnect in lockstep`() {
        val delays = (1..20).map { (connected().messages.first() as SseMessage.Retry).backoff }

        assertThat(delays, allElements(greaterThanOrEqualTo(Duration.ofMillis(500)) and lessThan(Duration.ofMillis(5000))))
        assertThat(delays.toSet().size, greaterThan(1))
    }

    @Test
    fun `a quiet stream is pinged every two seconds`() {
        val stream = connected()

        scheduler.tick(Duration.ofSeconds(6))

        assertThat(stream.pings, equalTo(3))
    }

    @Test
    fun `a node's settings choose how often its streams are pinged`() {
        val every10Seconds = AppInfra(AppRuntime(scheduler = scheduler), env = Environment.from("FANBELT_STREAM_KEEPALIVE" to "PT10S")).apply { start() }
        val page = Page(acmeWorkspace(every10Seconds), Layout(listOf(region), { "" }) { _, _ -> Shown("") })
        val stream = connected(page)

        scheduler.tick(Duration.ofSeconds(20))

        assertThat(stream.pings, equalTo(2))
    }

    @Test
    fun `a ping that can't be written closes the stream and ends the viewer's presence, and it stays ended`() {
        val joining = Page(workspace, Layout(listOf(region), { "" }) { _, _ -> Shown("") }, Viewing { scope, _, _ -> scope.presence.join(here, ann) })
        val stream = connected(joining, dead = true)
        assertThat(present(), equalTo(setOf(ann)))

        scheduler.tick(Duration.ofSeconds(2))
        assertThat(stream.closed, equalTo(true))
        assertThat(present(), equalTo(emptySet<PresenceMember>()))

        scheduler.tick(Duration.ofSeconds(40))
        assertThat(present(), equalTo(emptySet<PresenceMember>()))
    }

    @Test
    fun `a browser that leaves while its stream is being set up leaves no presence and no region watching for it`() {
        val reads = AtomicInteger()
        val counted = DerivedState<Unit, String>("counted") { ctx, _ ->
            ctx.watch(Topic.of("t"))
            reads.incrementAndGet()
            "v1"
        }
        val watched = Region("r", { """<p id="r">${(it as Shown).value}</p>""" }, counted) { model, _ -> Shown(model) }
        val joining = Page(workspace, Layout(listOf(watched), { "" }) { _, _ -> Shown("") }, Viewing { scope, _, _ -> scope.presence.join(here, ann) })

        joining.connect(Request(GET, "/"), gone = true)
        scheduler.runUntilIdle()
        val readsAfterLeaving = reads.get()

        workspace.scope(Request(GET, "/")).bus.publish(Topic.of("t"))
        scheduler.runUntilIdle()

        assertThat(present(), equalTo(emptySet<PresenceMember>()))
        assertThat(reads.get(), equalTo(readsAfterLeaving))
    }

    @Test
    fun `a closed stream is pinged no more`() {
        val stream = connected()
        scheduler.tick(Duration.ofSeconds(2))

        stream.close()
        scheduler.tick(Duration.ofSeconds(6))

        assertThat(stream.pings, equalTo(1))
    }
}
