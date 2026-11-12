/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.input.ErrorSignals
import org.http4k.fanbelt.input.FieldName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProvesServerRefusesTest {
    private val viewer = Cookie("viewer", "ann")

    @Test
    fun `a 403 is denied, for a post sent as datastar would send it with the viewer's cookies`() {
        val seen = mutableListOf<Request>()
        val refusing: HttpHandler = { request ->
            seen += request
            Response(FORBIDDEN)
        }

        assertEquals(Verdict.Denied, refusing.provesServerRefuses("/hidden", listOf(viewer), """{"a":1}"""))

        val request = seen.single()
        assertEquals(POST, request.method)
        assertEquals("/hidden", request.uri.path)
        assertEquals("ann", request.cookie("viewer")?.value)
        assertEquals("true", request.header("datastar-request"))
        assertEquals("""{"a":1}""", request.bodyString())
    }

    @Test
    fun `an error in place of the change is rejected`() {
        val rejecting: HttpHandler = { ErrorSignals().rejected(Outcome.Rejected(FieldName.of("text"), "No such channel")) }

        assertEquals(Verdict.Rejected("No such channel"), rejecting.provesServerRefuses("/hidden", listOf(viewer)))
    }

    @Test
    fun `any other answer fails, because the page hid something the server would allow`() {
        val allowing: HttpHandler = { Response(OK) }

        assertThrows<IllegalStateException> { allowing.provesServerRefuses("/hidden", listOf(viewer)) }
    }
}
