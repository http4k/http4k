package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.junit.jupiter.api.Test

class TestRuntimeTest {
    @Test
    fun `an outbound call with no fake wired fails loudly instead of reaching the network`() {
        val response = TestRuntime().http(Request(POST, "https://email.example/send"))

        assertThat(response.status, equalTo(SERVICE_UNAVAILABLE))
        assertThat(response.bodyString(), equalTo("no fake wired for POST https://email.example/send"))
    }
}
