/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
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
import org.junit.jupiter.api.Test

class ProvesServerRefusesTest {
    private val viewer = Cookie("viewer", "ann")

    @Test
    fun `a 403 is denied, for a post sent as datastar would send it with the viewer's cookies`() {
        val seen = mutableListOf<Request>()
        val refusing: HttpHandler = { request ->
            seen += request
            Response(FORBIDDEN)
        }

        assertThat(refusing.provesServerRefuses("/hidden", listOf(viewer), """{"a":1}"""), equalTo(Verdict.Denied))

        val request = seen.single()
        assertThat(request.method, equalTo(POST))
        assertThat(request.uri.path, equalTo("/hidden"))
        assertThat(request.cookie("viewer")?.value, equalTo("ann"))
        assertThat(request.header("datastar-request"), equalTo("true"))
        assertThat(request.bodyString(), equalTo("""{"a":1}"""))
    }

    @Test
    fun `an error in place of the change is rejected`() {
        val rejecting: HttpHandler = { ErrorSignals().rejected(Outcome.Rejected(FieldName.of("text"), "No such channel")) }

        assertThat(rejecting.provesServerRefuses("/hidden", listOf(viewer)), equalTo(Verdict.Rejected("No such channel")))
    }

    @Test
    fun `any other answer fails, because the page hid something the server would allow`() {
        val allowing: HttpHandler = { Response(OK) }

        assertThat({ allowing.provesServerRefuses("/hidden", listOf(viewer)) }, throws<IllegalStateException>())
    }
}
