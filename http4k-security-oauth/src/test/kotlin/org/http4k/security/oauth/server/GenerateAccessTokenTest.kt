package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.util.*

class GenerateAccessTokenTest {

    private val codes = InMemoryAuthorizationCodes()
    private val request = AuthorizationRequest(ClientId("a-clientId"), listOf(), Uri.of("redirect"), "state")
    private val code = codes.create(request)
    private val handler = GenerateAccessToken(HardcodedClientValidator(request.client, request.redirectUri, "a-secret"), codes, DummyAccessTokens())

    @Test
    fun `generates a dummy token`() {
        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", code.value)
            .form("client_id", request.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", request.redirectUri.toString())
        )

        assertThat(response, hasStatus(OK) and hasBody("dummy-access-token"))
        assertThat(codes.available(code), equalTo(false))
    }

    @Test
    fun `handles invalid grant_type`() {
        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "something_else")
            .form("code", code.value)
            .form("client_id", request.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", request.redirectUri.toString())
        )

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody("Invalid grant type"))
    }

    @Test
    fun `handles invalid client credentials`() {
        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", code.value)
            .form("client_id", request.client.value)
            .form("client_secret", "wrong-secret")
            .form("redirect_uri", request.redirectUri.toString())
        )

        assertThat(response, hasStatus(UNAUTHORIZED) and hasBody("Invalid client credentials"))
    }
}

private class InMemoryAuthorizationCodes : AuthorizationCodes {
    private val codes = mutableMapOf<AuthorizationCode, AuthorizationRequest>()

    override fun destroy(authorizationCode: AuthorizationCode) {
        codes.remove(authorizationCode)
    }

    override fun create(authorizationRequest: AuthorizationRequest): AuthorizationCode =
        AuthorizationCode(UUID.randomUUID().toString()).also {
            codes[it] = authorizationRequest
        }

    fun available(authorizationCode: AuthorizationCode) = codes.containsKey(authorizationCode)

}