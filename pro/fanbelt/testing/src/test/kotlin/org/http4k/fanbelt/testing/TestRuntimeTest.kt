/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestRuntimeTest {
    @Test
    fun `an outbound call with no fake wired fails loudly instead of reaching the network`() {
        val response = TestRuntime().http(Request(POST, "https://email.example/send"))

        assertEquals(SERVICE_UNAVAILABLE, response.status)
        assertEquals("no fake wired for POST https://email.example/send", response.bodyString())
    }
}
