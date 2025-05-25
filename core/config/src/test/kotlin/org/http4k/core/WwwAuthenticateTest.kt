package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class WwwAuthenticateTest {

    @Test
    fun `roundtrip from header value`() = runBlocking {
        val headerValue = "Bearer realm=\"example.com\", error=\"invalid_token\", error_description=\"The access token expired\""
        val parsed = WwwAuthenticate.parseHeader(headerValue)
        val expected = WwwAuthenticate(
            token = "Bearer",
            contents = mapOf(
                "realm" to "example.com",
                "error" to "invalid_token",
                "error_description" to "The access token expired"
            )
        )

        assertThat(parsed, equalTo(expected))
        assertThat(expected.toHeaderValue(), equalTo(headerValue))
    }

    @Test
    fun `when no map values`() = runBlocking {
        val headerValue = "Bearer"
        val parsed = WwwAuthenticate.parseHeader(headerValue)
        val expected = WwwAuthenticate(token = "Bearer", contents = mapOf())

        assertThat(parsed, equalTo(expected))
        assertThat(expected.toHeaderValue(), equalTo(headerValue))
    }
}
