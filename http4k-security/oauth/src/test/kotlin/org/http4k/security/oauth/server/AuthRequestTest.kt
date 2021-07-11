package org.http4k.security.oauth.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AuthRequestTest {

    @Test
    fun `is not open id connect auth request if scope doesn't include 'openid'`() {
        assertThat(defaultAuthRequest.copy(scopes = emptyList()).isOIDC(), equalTo(false))
        assertThat(defaultAuthRequest.copy(scopes = listOf("contacts", "public", "oauth")).isOIDC(), equalTo(false))
    }

    @Test
    fun `is open id connect auth request if scope includes 'openid'`() {
        assertThat(defaultAuthRequest.copy(scopes = listOf("openid")).isOIDC(), equalTo(true))
        assertThat(defaultAuthRequest.copy(scopes = listOf("OPENID")).isOIDC(), equalTo(true))
        assertThat(defaultAuthRequest.copy(scopes = listOf("OpEnID")).isOIDC(), equalTo(true))
    }

    private val defaultAuthRequest = AuthRequest(
        client = ClientId(UUID.randomUUID().toString()),
        redirectUri = Uri.of("http://someredirecturi"),
        responseType = ResponseType.Code,
        state = State("some state"),
        scopes = emptyList())
}
