/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.join
import org.http4k.fanbelt.state.set
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.lens.Query
import org.http4k.lens.value
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class PresenceRouteTest {
    private class Who private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Who>(::Who)
    }

    private class Cell private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Cell>(::Cell)
    }

    private object Sheet : Feature<Unit, Unit>({}) {
        val cursor by presence(Who, Cell::class)
    }

    private val cell = Query.value(Cell).required("cell")
    private val infra = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH))).apply { start() }
    private val workspace = acmeWorkspace(infra)
    private val scope = infra.scope(TenantId.of("acme"))
    private val alex = Who.of("alex")

    @Test
    fun `a presence value binds to a route like an operation does, and answers 204`() {
        Sheet.cursor.join(scope, alex)
        val route = Sheet.cursor.set(workspace, { alex }) { cell(it) }

        val response = route(Request(POST, "/cursor").query("cell", "B2"))

        assertEquals(NO_CONTENT, response.status)
        assertEquals(Cell.of("B2"), scope.read(Sheet.cursor)[alex])
    }

    @Test
    fun `a value that doesn't parse is a bad request`() {
        Sheet.cursor.join(scope, alex)
        val route = Sheet.cursor.set(workspace, { alex }) { cell(it) }

        assertEquals(BAD_REQUEST, route(Request(POST, "/cursor")).status)
    }

    @Test
    fun `a presence with no key is joined and set without one`() {
        Sheet.cursor.join(scope, alex)

        Sheet.cursor.set(scope, alex, Cell.of("C3"))

        assertEquals(Cell.of("C3"), scope.read(Sheet.cursor)[alex])
    }
}
