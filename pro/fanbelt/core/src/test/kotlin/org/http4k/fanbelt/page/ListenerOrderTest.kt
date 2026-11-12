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

class ListenerOrderTest {
    private data class Shown(val value: Int) : ViewModel

    @Test
    fun `a region redraws for its watchers in the order they arrived, so a run can be replayed`() {
        val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T09:00:00Z"))
        val scope = AppInfra(AppRuntime(scheduler = scheduler, events = {})).apply { start() }.scope(TenantId.of("acme"))
        var value = 1
        val state = DerivedState<Unit, Int>("n") { ctx, _ -> ctx.watch(Topic.of("t")); value }
        val region = Region("n", { """<p id="n">${(it as Shown).value}</p>""" }, state) { n, _ -> Shown(n) }
        val heard = mutableListOf<Int>()
        (1..50).forEach { watcher -> region.watch(scope, Request(GET, "/")) { if (it.html.contains(">2<")) heard += watcher } }
        scheduler.runUntilIdle()

        value = 2
        scope.bus.publish(Topic.of("t"))
        scheduler.runUntilIdle()

        assertEquals((1..50).toList(), heard)
    }
}
