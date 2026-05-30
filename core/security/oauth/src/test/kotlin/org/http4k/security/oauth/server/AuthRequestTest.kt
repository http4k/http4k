package org.http4k.security.oauth.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import org.http4k.core.Method.GET
import org.http4k.core.Request
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

    @Test
    fun `extracts code_challenge from query string`() {
        val request = baseAuthorizeRequest()
            .query("code_challenge", "abc123")
            .query("code_challenge_method", "S256")

        val authRequest = request.authorizationRequest()

        assertThat(authRequest.codeChallenge, equalTo("abc123"))
    }

    @Test
    fun `code_challenge is null when absent from query string`() {
        val authRequest = baseAuthorizeRequest().authorizationRequest()

        assertThat(authRequest.codeChallenge, equalTo(null))
    }

    @Test
    fun `rejects code_challenge_method other than S256 without leaking the value`() {
        listOf("plain", "bogus").forEach { badMethod ->
            val request = baseAuthorizeRequest()
                .query("code_challenge", "abc123")
                .query("code_challenge_method", badMethod)

            val result = AuthRequestFromQueryParameters.extract(request)

            val failure = result as? Failure
                ?: error("expected Failure for code_challenge_method=$badMethod, got $result")
            assertThat(failure.reason.reason.contains(badMethod), equalTo(false))
        }
    }

    @Test
    fun `accepts code_challenge_method=S256 case-insensitively`() {
        listOf("S256", "s256").forEach { method ->
            val request = baseAuthorizeRequest()
                .query("code_challenge", "abc123")
                .query("code_challenge_method", method)

            request.authorizationRequest()
        }
    }

    private fun baseAuthorizeRequest() = Request(GET, "/authorize")
        .query("client_id", "a-client")
        .query("response_type", "code")
        .query("scope", "openid")
        .query("redirect_uri", "https://app.example.com/cb")

    private val defaultAuthRequest = AuthRequest(
        client = ClientId(UUID.randomUUID().toString()),
        redirectUri = Uri.of("http://someredirecturi"),
        responseType = ResponseType.Code,
        state = State("some state"),
        scopes = emptyList())
}
