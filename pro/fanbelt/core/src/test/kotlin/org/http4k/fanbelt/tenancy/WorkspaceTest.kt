/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.tenancy

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class WorkspaceTest {
    private val created = setOf(TenantId.of("acme"))

    private val workspace = Workspace(
        AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH))).apply { start() },
        "/lists/{list}",
        created::contains,
    )

    private val app = workspace.routes(
        poly(
            routes(
                "/" bind GET to { request -> Response(OK).body(workspace.scope(request).tenant.value) },
                "/todos" bind POST to { Response(OK) },
            ),
            "/" bind sse { it.close() },
        )
    )

    private val http = app.http!!
    private val stream = app.sse!!

    @Test
    fun `a tenant that was created reaches its routes, with its own scope`() {
        val response = http(Request(GET, "/lists/acme/"))

        assertEquals(OK, response.status)
        assertEquals("acme", response.bodyString())
        assertEquals(OK, http(Request(POST, "/lists/acme/todos")).status)
        assertEquals(OK, stream(Request(GET, "/lists/acme/")).status)
    }

    @Test
    fun `a tenant that was never created is not found, for pages, commands and streams`() {
        assertEquals(NOT_FOUND, http(Request(GET, "/lists/ghost/")).status)
        assertEquals(NOT_FOUND, http(Request(POST, "/lists/ghost/todos")).status)
        assertEquals(NOT_FOUND, stream(Request(GET, "/lists/ghost/")).status)
    }

    @Test
    fun `a tenant name that doesn't parse is not found`() {
        assertEquals(NOT_FOUND, http(Request(GET, "/lists/not%20a%20name/")).status)
        assertEquals(NOT_FOUND, stream(Request(GET, "/lists/not%20a%20name/")).status)
    }

    @Test
    fun `the tenant's root is built from the template`() {
        assertEquals(Uri.of("/lists/acme/"), workspace.uri(TenantId.of("acme")))
    }
}
