/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.events

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.RegionId
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.ViewModel
import org.http4k.testing.RecordingEvents
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class TraceTest {
    private object Seats : Feature<Unit, String>({ }) {
        val reserve by operation { _, fan, seat: String ->
            when (fan) {
                "nobody" -> Denied
                "late" -> Rejected(FieldName.of("seat"), "Someone just took $seat")
                else -> Changes.of(Topic.of("seats"))
            }
        }
    }

    private val acme = TenantId.of("acme")
    private val events = RecordingEvents()
    private val scope = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH), events = events)).apply { start() }
        .scope(acme)

    @Test
    fun `an operation says, within its tenant, what it was called, who acted, and how it turned out`() {
        scope.execute(Seats.reserve, "ann", "F12")
        scope.execute(Seats.reserve, "late", "F12")
        scope.execute(Seats.reserve, "nobody", "F12")

        assertEquals(
            listOf(
                acme to Executed("reserve", "ann", Changes.of(Topic.of("seats"))),
                acme to Executed("reserve", "late", Rejected(FieldName.of("seat"), "Someone just took F12")),
                acme to Executed("reserve", "nobody", Denied),
            ),
            traced<Executed>(),
        )
    }

    @Test
    fun `a change is published to its topic within its tenant after the operation says how it turned out, and a refusal publishes nothing`() {
        scope.execute(Seats.reserve, "ann", "F12")
        scope.execute(Seats.reserve, "late", "F12")

        assertEquals(listOf(acme to Published(Topic.of("seats"))), traced<Published>())
        assertEquals(listOf(Executed::class, Published::class, Executed::class), traced<FanbeltTrace>().map { it.second::class })
    }

    @Test
    fun `a region says when it redrew, and when a change left it as it was`() {
        val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T09:00:00Z"))
        val scope = AppInfra(AppRuntime(scheduler = scheduler, events = events)).apply { start() }.scope(acme)
        var taken = setOf<String>()
        val seats = DerivedState<Unit, Set<String>>("seats") { ctx, _ -> ctx.watch(Topic.of("seats")); taken }
        val region = Region("seats", { """<ul id="seats"></ul>""" }, seats) { _, _ -> SeatsView }

        region.watch(scope, Request(GET, "/")) { }
        scheduler.runUntilIdle()
        scope.bus.publish(Topic.of("seats"))
        scheduler.runUntilIdle()
        taken = setOf("F12")
        scope.bus.publish(Topic.of("seats"))
        scheduler.runUntilIdle()

        assertEquals(
            listOf(true, false, true).map { acme to Redrawn(RegionId.of("seats"), "", it) },
            traced<Redrawn>(),
        )
    }

    @Test
    fun `each browser says who it is for, which regions it patched, and which it skipped as already shown`() {
        val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T09:00:00Z"))
        val infra = AppInfra(AppRuntime(scheduler = scheduler, events = events)).apply { start() }
        val workspace = Workspace(infra, "/", { true }, { acme }, { Uri.of("/") })
        val seats = DerivedState<Unit, String>("seats") { _, _ -> "F12" }
        val region = Region("seats", { """<ul id="seats"></ul>""" }, seats) { _, _ -> SeatsView }
        val page = Page(workspace, Layout(listOf(region), { (it as Shell).signals }) { _, shell -> Shell(shell.signals) }, Viewing({ request -> request.query("who")!! }))

        page.connect(Request(GET, "/?who=ann"))
        page.connect(Request(GET, "/?who=bob"))
        page.connect(Request(GET, "/?who=ann").query("datastar", page.http(Request(GET, "/")).bodyString()))
        scheduler.runUntilIdle()

        val patched = traced<Patched>().map { (tenant, trace) -> trace.also { assertEquals(acme, tenant) } }
        assertEquals(listOf(true, true, false), patched.map { it.sent })
        assertEquals(listOf("ann", "bob", "ann"), patched.map { it.actor })
        assertEquals(setOf(RegionId.of("seats")), patched.map { it.region }.toSet())
    }

    @Test
    fun `a browser on a page with no actor is for nobody in particular`() {
        val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T09:00:00Z"))
        val infra = AppInfra(AppRuntime(scheduler = scheduler, events = events)).apply { start() }
        val workspace = Workspace(infra, "/", { true }, { acme }, { Uri.of("/") })
        val region = Region("seats", { """<ul id="seats"></ul>""" }, DerivedState<Unit, String>("seats") { _, _ -> "F12" }) { _, _ -> SeatsView }
        val page = Page(workspace, Layout(listOf(region), { (it as Shell).signals }) { _, shell -> Shell(shell.signals) })

        page.connect(Request(GET, "/"))
        scheduler.runUntilIdle()

        assertEquals(listOf(null), traced<Patched>().map { it.second.actor })
    }

    private inline fun <reified T : Event> traced() =
        events.filterIsInstance<MetadataEvent>().filter { it.event is T }.map { eventTenant(it) to it.event as T }

    private data object SeatsView : ViewModel

    private data class Shell(val signals: String) : ViewModel
}
