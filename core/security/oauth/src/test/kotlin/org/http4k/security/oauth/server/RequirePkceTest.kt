package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.format.Jackson
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.security.State
import org.http4k.security.oauth.server.accesstoken.ClientSecretAccessTokenRequestAuthentication
import org.http4k.security.oauth.server.accesstoken.GrantTypesConfiguration
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test

class RequirePkceTest {

    private val clientId = ClientId("a-clientId")
    private val redirectUri = Uri.of("http://callback")
    private val authRequest = AuthRequest(clientId, listOf(), redirectUri, State("state"))
    private val clientValidator = HardcodedClientValidator(clientId, redirectUri, "a-secret")

    @Test
    fun `RequirePkce decorator rejects authorize request without code_challenge`() {
        val decorated = RequirePkce(AuthoriseRequestValidator.AlwaysValid)

        val result = decorated.validate(Request(GET, "/authorize"), authRequest)

        val failure = result as? Failure<*> ?: error("expected Failure, got $result")
        assertThat(failure.reason is InvalidAuthorizationRequest, equalTo(true))
    }

    @Test
    fun `RequirePkce decorator accepts authorize request with code_challenge`() {
        val decorated = RequirePkce(AuthoriseRequestValidator.AlwaysValid)
        val request = Request(GET, "/authorize")

        val result = decorated.validate(request, authRequest.copy(codeChallenge = "abc"))

        assertThat(result is Success<*>, equalTo(true))
    }

    @Test
    fun `OAuthServer with requirePkce=true rejects authorize request that lacks code_challenge`() {
        val server = oAuthServer(requirePkce = true)
        val handler = server.authenticationStart.then { Response(OK).body("ok") }

        val response = handler(
            Request(GET, "/authorize")
                .query("client_id", clientId.value)
                .query("response_type", "code")
                .query("redirect_uri", redirectUri.toString())
        )

        assertThat(response.header("location")!!, containsSubstring("error=invalid_request"))
    }

    @Test
    fun `OAuthServer with requirePkce=true accepts authorize request with code_challenge`() {
        val server = oAuthServer(requirePkce = true)
        val handler = server.authenticationStart.then { Response(OK).body("ok") }

        val response = handler(
            Request(GET, "/authorize")
                .query("client_id", clientId.value)
                .query("response_type", "code")
                .query("redirect_uri", redirectUri.toString())
                .query("code_challenge", "abc")
        )

        assertThat(response, hasStatus(OK))
    }

    @Test
    fun `token endpoint with requirePkce=true rejects stored code that has null challenge`() {
        val codes = InMemoryAuthorizationCodes(FixedClock)
        val code = codes.create(Request(GET, "/"), authRequest, Response(OK)).get()
        val handler = GenerateAccessToken(
            codes, DummyAccessTokens(), FixedClock, DummyIdTokens(), DummyRefreshTokens(),
            JsonResponseErrorRenderer(Jackson),
            GrantTypesConfiguration.default(ClientSecretAccessTokenRequestAuthentication(clientValidator)),
            requirePkce = true
        )

        val response = handler(
            Request(POST, "/token")
                .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
                .form("grant_type", "authorization_code")
                .form("code", code.value)
                .form("client_id", clientId.value)
                .form("client_secret", "a-secret")
                .form("redirect_uri", redirectUri.toString())
                .form("code_verifier", "anything")
        )

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(containsSubstring("\"error\":\"invalid_grant\"")))
    }

    @Test
    fun `token endpoint with requirePkce=false accepts stored code that has null challenge (default backward-compat)`() {
        val codes = InMemoryAuthorizationCodes(FixedClock)
        val code = codes.create(Request(GET, "/"), authRequest, Response(OK)).get()
        val handler = GenerateAccessToken(
            codes, DummyAccessTokens(), FixedClock, DummyIdTokens(), DummyRefreshTokens(),
            JsonResponseErrorRenderer(Jackson),
            GrantTypesConfiguration.default(ClientSecretAccessTokenRequestAuthentication(clientValidator))
        )

        val response = handler(
            Request(POST, "/token")
                .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
                .form("grant_type", "authorization_code")
                .form("code", code.value)
                .form("client_id", clientId.value)
                .form("client_secret", "a-secret")
                .form("redirect_uri", redirectUri.toString())
        )

        assertThat(response, hasStatus(OK))
    }

    private fun oAuthServer(requirePkce: Boolean) =
        customOAuthServer(clientValidator = clientValidator, requirePkce = requirePkce)
}
