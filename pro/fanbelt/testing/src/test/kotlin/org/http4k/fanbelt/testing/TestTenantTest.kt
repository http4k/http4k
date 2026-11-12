/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.state.StateContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class TestTenantTest {
    private val tenant = TestTenant()
    private val closesAt = tenant.clock.instant() + Duration.ofMinutes(10)

    @Test
    fun `a read that changes once its stale time has passed isn't a missed publish`() {
        val lot = Lot { ctx ->
            if (tenant.clock.instant() < closesAt) "open".also { ctx.staleAt(closesAt) } else "closed"
        }.lot
        tenant.read(lot)

        tenant.tick(Duration.ofMinutes(11))

        assertEquals("closed", tenant.read(lot))
    }

    @Test
    fun `a read that changes before its stale time without a publish is still caught`() {
        val value = AtomicReference("open")
        val lot = Lot { ctx -> value.get().also { ctx.staleAt(closesAt) } }.lot
        tenant.read(lot)

        value.set("closed")
        tenant.tick(Duration.ofMinutes(1))

        assertThrows(IllegalStateException::class.java) { tenant.read(lot) }
    }

    private class Lot(read: (StateContext<Unit>) -> String) : Feature<Unit, Unit>({}) {
        val lot by derived { ctx, _: Unit -> read(ctx) }
    }
}
