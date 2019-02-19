package org.http4k.security.oauth.server

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.Test

class GenerateAccessTokenTest {

    @Test
    fun `generates a dummy token`() {
        val handler = GenerateAccessToken(DummyAccessTokens())
        val response = handler(Request(Method.POST, "/token"))
        assertThat(response, hasStatus(Status.OK))
        assertThat(response, hasBody("dummy-access-token"))
    }
}