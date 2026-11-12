/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.SchedulerClock
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class StaleAtTest {
    private data class Shown(val value: String) : ViewModel

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T09:00:00Z"))
    private val clock = SchedulerClock(scheduler)
    private val scope = AppInfra(AppRuntime(clock = clock, scheduler = scheduler)).apply { start() }.scope(TenantId.of("acme"))
    private val closesAt = clock.instant() + Duration.ofSeconds(5)
    private val reads = AtomicInteger()

    private val lot = DerivedState<Unit, String>("lot") { ctx, _ ->
        reads.incrementAndGet()
        if (clock.instant() < closesAt) {
            ctx.staleAt(closesAt)
            "open"
        } else {
            "closed"
        }
    }

    private val region = Region("lot", { """<p id="lot">${(it as Shown).value}</p>""" }, lot) { value, _ -> Shown(value) }

    @Test
    fun `a read that says when it goes stale is read again then, with nothing published`() {
        val sent = CopyOnWriteArrayList<String>()
        region.watch(scope, Request(GET, "/")) { sent += it.html }

        scheduler.tick(Duration.ofSeconds(4))
        assertEquals(listOf("""<p id="lot">open</p>"""), sent)

        scheduler.tick(Duration.ofSeconds(1))
        assertEquals("""<p id="lot">closed</p>""", sent.last())
    }

    @Test
    fun `a region nobody watches isn't read when its read goes stale`() {
        region.watch(scope, Request(GET, "/")) {}.close()
        val afterLeaving = reads.get()

        scheduler.tick(Duration.ofSeconds(5))

        assertEquals(afterLeaving, reads.get())
    }
}
