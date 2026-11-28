package org.http4k.fanbelt.testing.infrastructure

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.junit.jupiter.api.Test

class TestRuntimeTest {
    @Test
    fun `an outbound call with no fake wired fails loudly instead of reaching the network`() {
        val response = TestRuntime().http(Request(POST, "https://email.example/send"))

        assertThat(response.status, equalTo(NOT_IMPLEMENTED))
    }
}
