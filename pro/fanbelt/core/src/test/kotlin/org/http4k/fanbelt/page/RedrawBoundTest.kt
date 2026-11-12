/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.config.Environment
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.FanbeltSettings.FANBELT_REDRAW_LIMIT
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.tenancy.RedrawLimit
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RedrawBoundTest {
    private data class Shown(val value: Int) : ViewModel

    private data class Redrawn(val mostAtOnce: Int, val events: List<Event>)

    private fun redrawAll(env: Environment, keys: Int): Redrawn {
        val events = CopyOnWriteArrayList<Event>()
        val infra = AppInfra(AppRuntime(scheduler = Executors.newScheduledThreadPool(keys), events = { events += if (it is MetadataEvent) it.event else it }), env = env).apply { start() }
        val scope = infra.scope(TenantId.of("acme"))
        val topic = Topic.of("t")
        val changed = AtomicBoolean(false)
        val reading = AtomicInteger()
        val mostAtOnce = AtomicInteger()
        val redrawn = CountDownLatch(keys)

        val state = DerivedState<Int, Int>("slow") { ctx, key ->
            ctx.watch(topic)
            if (changed.get()) {
                mostAtOnce.accumulateAndGet(reading.incrementAndGet(), ::maxOf)
                Thread.sleep(50)
                reading.decrementAndGet()
                -key
            } else {
                key
            }
        }
        val region = Region("slow", { """<p id="slow">${(it as Shown).value}</p>""" }, { request: Request -> request.query("k")!!.toInt() }, state) { value, _ -> Shown(value) }

        (1..keys).forEach { key -> region.watch(scope, Request(GET, "/?k=$key")) { if (it.html.contains("-")) redrawn.countDown() } }
        changed.set(true)
        infra.bus.publish(scope.tenant(topic))

        assertTrue(redrawn.await(10, SECONDS))
        infra.close()
        return Redrawn(mostAtOnce.get(), events)
    }

    @Test
    fun `a node redraws at most its bound at once, and the rest wait their turn, saying how long`() {
        val redrawn = redrawAll(Environment.EMPTY.with(FANBELT_REDRAW_LIMIT of RedrawLimit.of(2)), keys = 10)

        assertEquals(2, redrawn.mostAtOnce)
        val waits = redrawn.events.filterIsInstance<RedrawWaited>()
        assertEquals(true, waits.isNotEmpty())
        assertEquals(setOf(RegionId.of("slow")), waits.map { it.region }.toSet())
        assertEquals(true, waits.all { it.waited > Duration.ZERO })
    }

    @Test
    fun `a node redraws at most 8 at once unless its environment says otherwise`() {
        assertEquals(8, redrawAll(Environment.EMPTY, keys = 20).mostAtOnce)
    }
}
