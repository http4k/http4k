/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.events.Event
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.events.Executed
import org.http4k.fanbelt.events.Patched
import org.http4k.fanbelt.events.Published
import org.http4k.fanbelt.events.Redrawn
import org.http4k.fanbelt.events.eventTenant
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.page.RedrawWaited
import org.http4k.fanbelt.page.RegionId
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.Marble.Kind
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(ApprovalTest::class)
class MarblesTest {
    private val acme = TenantId.of("acme")

    @Test
    fun `each actor's operation, topic, region and actor's browser in a tenant gets a lane, and each trace a column, in the order they happened`() {
        val traces = listOf(
            Executed("reserve", "ann", Changes.of(setOf(Topic.of("seats")))),
            Published(Topic.of("seats")),
            RedrawWaited(RegionId.of("seats"), Duration.ofMillis(1)),
            Redrawn(RegionId.of("seats"), "", true),
            Patched(RegionId.of("seats"), "ann", true),
            Patched(RegionId.of("seats"), "bob", true),
            Executed("reserve", "bob", Rejected(FieldName.of("seat"), "Someone just took F12")),
            Executed("release", "bob", Denied),
            Published(Topic.of("seats")),
            Redrawn(RegionId.of("seats"), "", false),
        ).map { eventTenant(acme, it) }

        assertEquals(
            """
            ann reserve @acme  -✓----------------
            bob reserve @acme  -----------✗------
            bob release @acme  -------------⊘----
            seats @acme        ---●-----------●--
            #seats @acme       -----●-----------○
            ann #seats @acme   -------●----------
            bob #seats @acme   ---------●--------
            """.trimIndent(),
            ascii(Timeline.of(traces)),
        )
    }

    @Test
    fun `aliases make keys readable wherever they appear in a lane's name, longest first`() {
        val show = "bb1ad573"
        val traces = listOf(
            Published(Topic.of("bookings:$show")),
            Redrawn(RegionId.of("basket"), "Shopper(show=$show, fan=f00d)", false),
        )

        assertEquals(
            """
            bookings:Midnight Owls  -●--
            #basket(ann)            ---○
            """.trimIndent(),
            ascii(Timeline.of(traces, mapOf(show to "Midnight Owls", "Shopper(show=$show, fan=f00d)" to "ann"))),
        )
    }

    @Test
    fun `browsers whose page doesn't say who they are for share a lane, and aliases name who acted`() {
        val traces = listOf(
            Executed("reserve", "Fan(id=f00d)", Changes.of(emptySet())),
            Patched(RegionId.of("seats"), null, true),
            Patched(RegionId.of("seats"), null, false),
            Patched(RegionId.of("seats"), "Fan(id=f00d)", true),
        )

        assertEquals(
            """
            ann reserve     -✓------
            browser #seats  ---●-○--
            ann #seats      -------●
            """.trimIndent(),
            ascii(Timeline.of(traces, mapOf("Fan(id=f00d)" to "ann"))),
        )
    }

    @Test
    fun `the same thing in two tenants gets a lane in each`() {
        val traces = listOf<Event>(
            eventTenant(acme, Published(Topic.of("seats"))),
            eventTenant(TenantId.of("globex"), Published(Topic.of("seats"))),
        )

        assertEquals(
            """
            seats @acme    -●--
            seats @globex  ---●
            """.trimIndent(),
            ascii(Timeline.of(traces)),
        )
    }

    @Test
    fun `the html render draws one row per lane and every kind of marble, with dropped marbles hollow`(approver: Approver) {
        val timeline = Timeline(
            listOf(
                Lane("reserve", listOf(Marble(0, Kind.Changes), Marble(3, Kind.Rejected))),
                Lane("release", listOf(Marble(4, Kind.Denied))),
                Lane("#basket(<bob>)", listOf(Marble(1, Kind.Dropped))),
                Lane("browser 1 #seats", listOf(Marble(2, Kind.Happened))),
            ),
            columns = 5,
        )

        val render = HtmlMarbles().render("Seats - two fans", timeline)

        assertEquals("Seats - two fans" to "HTML", render.title to render.format)
        approver.assertApproved(render.content)
    }

    private fun ascii(timeline: Timeline) = AsciiMarbles().render("marbles", timeline).content
}
