/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.events.Event
import org.http4k.fanbelt.RecordingSse
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class RenderSharingTest {
    private data class Shown(val value: String, val viewer: String) : ViewModel

    private data class Stored(override val id: String, val value: String) : Identified<String>

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T00:00:00Z"))
    private val infra = AppInfra(AppRuntime(scheduler = scheduler)).apply { start() }
    private val topic = Topic.of("t")
    private val reads = AtomicInteger()
    private val values = TableName.of("values")
    private val workspace = Workspace(
        infra,
        template = "/",
        exists = { true },
        tenant = { TenantId.of(it.query("tenant") ?: "acme") },
        root = { Uri.of("/?tenant=$it") },
    )
    private val shown = DerivedState<String, String>("shown") { ctx, _ ->
        ctx.watch(topic)
        reads.incrementAndGet()
        ctx.tracker.tables.invoke<String, Stored>(values).get("v")!!.value
    }

    init {
        listOf("acme", "other").forEach { tenant -> infra.storage.write { it.tables(TenantId.of(tenant)).invoke<String, Stored>(values).insert(Stored("v", "v1")) } }
    }
    private val region = Region(
        "r", { """<p id="r">$it</p>""" },
        key = { _: Request -> "shared" },
        state = shown,
        view = { model, request -> Shown(model, request.query("viewer").orEmpty()) },
    )
    private val page = Page(workspace, Layout(listOf(region), { it.toString() }) { _, _ -> Shown("", "") })

    private fun connect(viewer: Int, tenant: String = "acme", page: Page = this.page): RecordingSse {
        val stream = page.connect(Request(GET, "/?viewer=$viewer&tenant=$tenant"))
        scheduler.runUntilIdle()
        return stream
    }

    private fun change(to: String, tenant: String = "acme") {
        infra.storage.write { it.tables(TenantId.of(tenant)).invoke<String, Stored>(values).update("v") { stored -> stored.copy(value = to) } }
        workspace.scope(Request(GET, "/?tenant=$tenant")).bus.publish(topic)
        scheduler.runUntilIdle()
    }

    @Test
    fun `one change reads a shared model once, and every connection gets its own view`() {
        val connections = (1..100).map(::connect)
        reads.set(0)

        change("v2")

        assertEquals(1, reads.get())
        connections.forEachIndexed { i, received ->
            assertEquals(2, received.received.size)
            assertEquals(true, received.received.last().contains("Shown(value=v2, viewer=${i + 1})"))
        }
    }

    @Test
    fun `a view that ignores the viewer is rendered once per change, however many are watching`() {
        val renders = AtomicInteger()
        val everyone = Region("everyone", { renders.incrementAndGet(); """<p id="everyone">$it</p>""" }, key = { _: Request -> "shared" }, state = shown) { model: String, _ ->
            Shown(model, "everyone")
        }
        val page = Page(workspace, Layout(listOf(everyone), { it.toString() }) { _, _ -> Shown("", "") })
        val connections = (1..100).map { connect(it, page = page) }
        renders.set(0)

        change("v2")

        assertEquals(1, renders.get())
        connections.forEach { assertEquals(true, it.received.last().contains("Shown(value=v2, viewer=everyone)")) }
    }

    @Test
    fun `an unchanged model reads once and sends nothing`() {
        val connections = (1..100).map(::connect)
        reads.set(0)

        change("v1")

        assertEquals(1, reads.get())
        connections.forEach { assertEquals(1, it.received.size) }
    }

    @Test
    fun `the same key in two tenants never shares a model or a change`() {
        val acme = connect(1, "acme")
        val other = connect(2, "other")

        change("v2", "other")

        assertEquals(1, acme.received.size)
        assertEquals(2, other.received.size)
        assertEquals(true, other.received.last().contains("Shown(value=v2, viewer=2)"))
    }

    @Test
    fun `every template a region switches to must carry the region's id, not just the first`() {
        val switching = Region(
            "switching",
            { if ((it as Shown).value == "gone") "<p>gone</p>" else """<p id="switching">here</p>""" },
            { _: Request -> "shared" },
            shown,
        ) { model, request -> Shown(request.query("show") ?: model, "") }
        val reported = CopyOnWriteArrayList<Event>()
        fun paint(request: Request) = workspace.scope(request).reading { ctx -> switching.firstPaint(ctx, request) { reported += it } }.value.html()

        paint(Request(GET, "/"))
        val gone = paint(Request(GET, "/?show=gone"))

        assertEquals("""<div id="switching"></div>""", gone)
        assertEquals(listOf("region switching must render one root element with id=\"switching\""), reported.filterIsInstance<RedrawFailed>().map { it.cause.message })
    }
}
