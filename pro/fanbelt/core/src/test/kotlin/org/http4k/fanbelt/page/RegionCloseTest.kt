/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RegionCloseTest {
    private data class Shown(val value: String) : ViewModel

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T09:00:00Z"))
    private val infra = AppInfra(AppRuntime(scheduler = scheduler)).apply { start() }
    private val scope = infra.scope(TenantId.of("acme"))
    private val before = Topic.of("before")
    private val after = Topic.of("after")
    private val moved = AtomicBoolean(false)
    private val reads = AtomicInteger()

    private val state = DerivedState<Unit, String>("watched") { ctx, _ ->
        reads.incrementAndGet()
        ctx.watch(if (moved.get()) after else before)
        "v${reads.get()}"
    }

    private val region = Region("watched", { """<p id="watched">${(it as Shown).value}</p>""" }, state) { value, _ -> Shown(value) }

    @Test
    fun `a redraw already queued when the last viewer leaves doesn't leave a subscription behind`() {
        val watching = region.watch(scope, Request(GET, "/")) {}

        moved.set(true)
        infra.bus.publish(scope.tenant(before))
        watching.close()
        scheduler.runUntilIdle()
        val afterLeaving = reads.get()

        infra.bus.publish(scope.tenant(after))
        scheduler.runUntilIdle()

        assertEquals(afterLeaving, reads.get())
    }
}
