/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.sse.SseMessage
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Random

class StreamRetryTest {
    private data class Shown(val text: String) : ViewModel

    private val workspace = acmeWorkspace(AppInfra(AppRuntime(random = Random(0))).apply { start() })
    private val region = Region("r", { """<p id="r">${(it as Shown).text}</p>""" }, { _: Request -> "k" }, DerivedState<String, String>("s") { _, _ -> "v" }) { model, _ -> Shown(model) }

    private val page = Page(workspace, Layout(listOf(region), { (it as Shown).text }) { _, shell -> Shown(shell.connect) })

    private fun firstFrame() = page.connect(Request(GET, "/live")).messages.first()

    @Test
    fun `the page's connect action closes while the page is hidden, so a page left in the back-forward cache gives its connection back, and keeps retrying after clean closes and gateway errors`() {
        assertEquals("@get('/live', {retry: 'always', retryMaxCount: 1000})", page.http(Request(GET, "/live")).bodyString())
    }

    @Test
    fun `every stream starts by telling the browser a jittered retry delay, so a node's clients don't reconnect in lockstep`() {
        val delays = (1..20).map { (firstFrame() as SseMessage.Retry).backoff }

        assertEquals(true, delays.all { it >= Duration.ofMillis(500) && it < Duration.ofMillis(5000) })
        assertEquals(true, delays.toSet().size > 1)
    }
}
