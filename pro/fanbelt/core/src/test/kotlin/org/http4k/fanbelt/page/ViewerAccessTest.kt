/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.fanbelt.RecordingSse
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.feature.Access
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.DerivedState
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ViewerAccessTest {
    private data class Shown(val text: String) : ViewModel

    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T00:00:00Z"))
    private val infra = AppInfra(AppRuntime(scheduler = scheduler)).apply { start() }
    private val workspace = acmeWorkspace(infra)
    private val staff = Topic.of("staff")
    private val access = ConcurrentHashMap(
        mapOf(
            "sam" to Allowed("sam"),
            "eve" to Denied,
            "ghost" to Hidden,
            "rex" to Rejected(FieldName.of("badge"), "Expired")
        )
    )

    private val member = DerivedState<ActorKey<String, Unit>, Access<String>>("member") { ctx, (viewer, _) ->
        ctx.watch(staff)
        access.getValue(viewer)
    }
    private val board = DerivedState<Unit, String>("board") { _, _ -> "the board" }
    private val region = Region("board", { """<p id="board">${(it as Shown).text}</p>""" }, board) { text, _ -> Shown(text) }
    private val viewer = { request: Request -> ActorKey(request.query("as")!!, Unit) }
    private val layout = Layout(listOf(region), { (it as Shown).text }) { _, shell -> Shown(shell.regions.values.joinToString("")) }
    private val page = Page(workspace, layout, Viewing(viewer, member))
    private val toLobby = Page(workspace, layout, Viewing(viewer, member, { Uri.of("/lobby") }))

    private fun revoke(from: Page): RecordingSse {
        val stream = from.connect(Request(GET, "/?as=sam"))
        scheduler.runUntilIdle()
        access["sam"] = Denied
        workspace.scope(Request(GET, "/")).bus.publish(staff)
        scheduler.runUntilIdle()
        return stream
    }

    @Test
    fun `a page whose rule has no key is given just the actor`() {
        val actor = { request: Request -> request.query("as")!! }
        val byActor = Page(workspace, layout, Viewing(actor, member))

        assertThat(byActor.http(Request(GET, "/?as=sam")).status, equalTo(OK))
        assertThat(byActor.http(Request(GET, "/?as=eve")).status, equalTo(FORBIDDEN))
    }

    @Test
    fun `a page's connect is given what its rule found`() {
        val connected = mutableListOf<String>()
        val badge = DerivedState<ActorKey<String, Unit>, Access<String>>("badge") { _, (viewer, _) -> Allowed("badge of $viewer") }
        val byBadge = Page(workspace, layout, Viewing(viewer, badge) { _, _, found -> connected += found; AutoCloseable {} })

        byBadge.connect(Request(GET, "/?as=sam"))
        scheduler.runUntilIdle()

        assertThat(connected, equalTo(listOf("badge of sam")))
    }

    @Test
    fun `a page with no rule is given the actor as it connects`() {
        val connected = mutableListOf<String>()
        val actor = { request: Request -> request.query("as")!! }
        val open = Page(workspace, layout, Viewing(actor) { _, _, found -> connected += found; AutoCloseable {} })

        open.connect(Request(GET, "/?as=sam"))
        scheduler.runUntilIdle()

        assertThat(connected, equalTo(listOf("sam")))
    }

    @Test
    fun `a page shows to a viewer its rule allows, and answers 403 to one it denies and 404 to one it hides`() {
        assertThat(page.http(Request(GET, "/?as=sam")).status, equalTo(OK))
        assertThat(page.http(Request(GET, "/?as=eve")).status, equalTo(FORBIDDEN))
        assertThat(page.http(Request(GET, "/?as=ghost")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `a rule that rejects a viewer is a 403 to a page, as a denial is`() {
        assertThat(page.http(Request(GET, "/?as=rex")).status, equalTo(FORBIDDEN))
        assertThat(page.sse(Request(GET, "/?as=rex")).status, equalTo(FORBIDDEN))
    }

    @Test
    fun `a refused viewer is sent home, and sees none of the page`() {
        val refused = page.http(Request(GET, "/?as=eve")).bodyString()

        assertThat(refused, containsSubstring("""window.location.assign("/")"""))
        assertThat(refused, !containsSubstring("the board"))
    }

    @Test
    fun `a viewer the rule refuses is refused a stream`() {
        assertThat(page.sse(Request(GET, "/?as=eve")).status, equalTo(FORBIDDEN))
        assertThat(page.sse(Request(GET, "/?as=ghost")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `an open page sends its viewer home when the rule stops allowing them`() {
        assertThat(revoke(page).received, anyElement(containsSubstring("""window.location.assign("/")""")))
    }

    @Test
    fun `a page can send a refused viewer somewhere else, whether arriving or watching`() {
        assertThat(toLobby.http(Request(GET, "/?as=eve")).bodyString(), containsSubstring("""window.location.assign("/lobby")"""))
        assertThat(revoke(toLobby).received, anyElement(containsSubstring("""window.location.assign("/lobby")""")))
    }

    @Test
    fun `a refused viewer's stream ends once they are sent away`() {
        assertThat(revoke(page).closed, equalTo(true))
    }

    @Test
    fun `an open page the rule still allows is not sent away`() {
        val stream = page.connect(Request(GET, "/?as=sam"))
        scheduler.runUntilIdle()

        workspace.scope(Request(GET, "/")).bus.publish(staff)
        scheduler.runUntilIdle()

        assertThat(stream.received.filter { it.contains("window.location.assign") }, isEmpty)
        assertThat(stream.closed, equalTo(false))
    }
}
