/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

class OperationBindingTest {
    private data class Signals(val note: String = "")

    private object Counter : Feature<Unit, Unit>({}) {
        val added = CopyOnWriteArrayList<Int>()

        val add by operation { _, _, n: Int ->
            added += n
            Changes.none()
        }
    }

    private val infra = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH))).apply { start() }
    private val workspace = acmeWorkspace(infra)
    private val n = Path.int().of("n")

    @Test
    fun `an operation with no actor is bound without one`() {
        Counter.added.clear()
        val app = routes(
            "/add/{n}" bind POST to Counter.add(workspace, input = n),
            "/signalled/{n}" bind POST to Counter.add(workspace, { _: Signals, request -> n(request) }),
        )

        app(Request(POST, "/add/3"))
        app(Request(POST, "/signalled/4").body("{}"))

        assertEquals(listOf(3, 4), Counter.added)
    }

    @Test
    fun `input that doesn't parse is a bad request, as it is for a page`() {
        val app = routes("/add/{n}" bind POST to Counter.add(workspace, input = n))

        assertEquals(BAD_REQUEST, app(Request(POST, "/add/three")).status)
    }
}
