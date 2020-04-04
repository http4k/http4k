package org.http4k.security.oauth.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.util.Failure
import org.http4k.util.Result
import org.http4k.util.Success
import org.junit.jupiter.api.Test

class SimpleAuthoriseRequestValidatorTest {
    private val validClientId = ClientId("a-client")
    private val validRedirectUri = Uri.of("https://a-redirect-uri")
    private val validScopes = listOf("openid", "profile")

    private val aRequest = Request(GET, "/some/path")

    private val authoriseRequestValidator = SimpleAuthoriseRequestValidator(HardcodedClientValidator(
        validClientId,
        validRedirectUri,
        expectedScopes = validScopes))


    @Test
    fun `return auth request when client is valid`() {
        val validAuthRequest = AuthRequest(
            responseType = ResponseType.Code,
            client = validClientId,
            redirectUri = validRedirectUri,
            scopes = validScopes,
            state = State("")
        )
        assertThat(authoriseRequestValidator.validate(aRequest, validAuthRequest),
            equalTo(success(aRequest)))
    }

    @Test
    fun `validates client_id value`() {
        val authRequest = AuthRequest(
            responseType = ResponseType.Code,
            client = ClientId("invalid-client"),
            redirectUri = validRedirectUri,
            scopes = validScopes,
            state = State("some state")
        )
        assertThat(authoriseRequestValidator.validate(aRequest, authRequest), equalTo(failure(InvalidClientId)))
    }

    @Test
    fun `validates redirect_uri value`() {
        val authRequest = AuthRequest(
            responseType = ResponseType.Code,
            client = validClientId,
            redirectUri = Uri.of("http://invalid.uri"),
            scopes = validScopes,
            state = State("some state")
        )
        assertThat(authoriseRequestValidator.validate(aRequest, authRequest), equalTo(failure(InvalidRedirectUri)))
    }

    @Test
    fun `validates scopes`() {
        val authRequest = AuthRequest(
            responseType = ResponseType.Code,
            client = validClientId,
            redirectUri = validRedirectUri,
            scopes = listOf("some", "invalid", "scopes"),
            state = State("some state")
        )
        assertThat(authoriseRequestValidator.validate(aRequest, authRequest), equalTo(failure(InvalidScopes)))
    }

    private fun success(request: Request): Result<OAuthError, Request> = Success(request)

    private fun failure(oAuthError: OAuthError): Result<OAuthError, Request> = Failure(oAuthError)
}
