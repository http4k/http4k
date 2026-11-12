/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.eventually
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.VirtualThreadScheduler
import org.http4k.fanbelt.state.DerivedState
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SlowRegionTest {
    private data class Shown(val value: String) : ViewModel

    private val executor = VirtualThreadScheduler()
    private val infra = AppInfra(AppRuntime(scheduler = executor)).apply { start() }
    private val topic = Topic.of("t")
    private val value = AtomicReference("v1")
    private val slow = AtomicBoolean(false)
    private val reading = CountDownLatch(1)
    private val released = CountDownLatch(1)
    private val stall = AtomicBoolean(false)
    private val stalled = CountDownLatch(1)
    private val unstalled = CountDownLatch(1)
    private val workspace = acmeWorkspace(infra)
    private val state = DerivedState<Unit, String>("shown") { ctx, _ ->
        ctx.watch(topic)
        if (slow.get()) {
            reading.countDown()
            released.await()
        }
        value.get()
    }
    private val region = Region("r", ::render, state) { model, _ -> Shown(model) }
    private val page = Page(workspace, Layout(listOf(region), { "" }) { _, _ -> Shown("") })

    private fun render(model: ViewModel): String {
        if (stall.compareAndSet(true, false)) {
            stalled.countDown()
            unstalled.await()
        }
        return """<p id="r">${(model as Shown).value}</p>"""
    }

    private fun connect(onSend: (String) -> Unit = {}) = page.connect(Request(GET, "/")) { onSend(it.toMessage()) }

    private fun change(to: String) {
        value.set(to)
        workspace.scope(Request(GET, "/")).bus.publish(topic)
        executor.submit {}.get()
    }

    @Test
    fun `a viewer connecting while a slow refresh holds the region gets the last view at once, then the new one`() {
        val first = connect()
        slow.set(true)
        value.set("v2")
        workspace.scope(Request(GET, "/")).bus.publish(topic)
        reading.await()

        try {
            val second = CompletableFuture.supplyAsync { connect() }.get(2, SECONDS)
            eventually { second.received.isNotEmpty() }
            assertTrue(second.received.last().contains("v1"), "connected with ${second.received.lastOrNull()}")

            released.countDown()
            eventually { first.received.lastOrNull().orEmpty().contains("v2") && second.received.lastOrNull().orEmpty().contains("v2") }
        } finally {
            released.countDown()
        }
    }

    @Test
    fun `a viewer whose first view is slow to render never ends up behind a newer one`() {
        val first = connect()
        stall.set(true)
        val second = CompletableFuture.supplyAsync { connect() }
        stalled.await()

        value.set("v2")
        workspace.scope(Request(GET, "/")).bus.publish(topic)
        eventually { first.received.lastOrNull().orEmpty().contains("v2") }
        unstalled.countDown()

        val received = second.get(2, SECONDS)
        eventually { first.received.lastOrNull().orEmpty().contains("v2") && received.received.lastOrNull().orEmpty().contains("v2") }
    }

    @Test
    fun `a stuck viewer doesn't hold up the others, and catches up with only the latest`() {
        val stuck = CountDownLatch(1)
        val slow = connect { if (it.contains("v2")) stuck.await() }
        val fast = connect()
        eventually { slow.received.size == 1 && fast.received.size == 1 }

        change("v2")
        eventually { fast.received.last().contains("v2") }
        change("v3")
        change("v4")
        eventually { fast.received.last().contains("v4") }

        stuck.countDown()
        eventually { slow.received.last().contains("v4") }
        assertTrue(slow.received.none { it.contains("v3") })
    }
}
