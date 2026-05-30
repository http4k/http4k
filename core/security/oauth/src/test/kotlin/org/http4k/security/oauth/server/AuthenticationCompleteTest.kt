package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseMode.Fragment
import org.http4k.security.ResponseMode.Query
import org.http4k.security.ResponseType
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.State
import org.http4k.security.fragmentParameter
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.junit.jupiter.api.Test

class AuthenticationCompleteTest {

    private val authorizationRequest =
        AuthRequest(
            ClientId("a-client-id"),
            listOf("email"),
            Uri.of("http://destination"),
            State("some state")
        )

    val underTest = AuthenticationComplete(
        DummyAuthorizationCodes(authorizationRequest, this::isFailure, "jdoe"),
        DummyOAuthAuthRequestTracking(),
        DummyIdTokens("jdoe")
    )

    private fun isFailure(request: Request): Boolean = request.query("fail") == "true"

    @Test
    fun `redirects on successful login`() {
        val response = underTest(Request(POST, "/login").withAuthorization(authorizationRequest))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri!!
                .query("code", "dummy-token-for-jdoe")
                .query("state", "some state").toString()))
    }

    @Test
    fun `redirects on successful login, with a fragment if requested`() {
        val response = underTest(Request(POST, "/login").withAuthorization(authorizationRequest.copy(responseMode = Fragment), responseMode = Fragment))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri!!
                .fragmentParameter("code", "dummy-token-for-jdoe")
                .fragmentParameter("state", "some state").toString()))
    }

    @Test
    fun `includes id_token if response_type requires it`() {
        val response = underTest(Request(POST, "/login").withAuthorization(authorizationRequest, CodeIdToken))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri!!
                .fragmentParameter("code", "dummy-token-for-jdoe")
                .fragmentParameter("id_token", "dummy-id-token-for-jdoe-nonce:unknown")
                .fragmentParameter("state", "some state").toString()))
    }

    @Test
    fun `includes id_token if response_type requires it, with code if requested`() {
        val response = underTest(Request(POST, "/login").withAuthorization(authorizationRequest, CodeIdToken, Query))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri!!
                .query("code", "dummy-token-for-jdoe")
                .query("id_token", "dummy-id-token-for-jdoe-nonce:unknown")
                .query("state", "some state").toString()))
    }

    @Test
    fun `redirects with error details if login is not successful`() {
        val response = underTest(Request(POST, "/login").withAuthorization(authorizationRequest).query("fail", "true"))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri!!
                .query("error", "access_denied")
                .query("error_description", UserRejectedRequest.description)
                .query("state", "some state").toString()))
    }

    @Test
    fun `redirects with error details including error_uri if provided`() {
        val errorUri = "SomeUri"
        val underTest = AuthenticationComplete(
            DummyAuthorizationCodes(authorizationRequest, this::isFailure, "jdoe"),
            DummyOAuthAuthRequestTracking(),
            DummyIdTokens("jdoe"),
            errorUri
        )
        val response = underTest(Request(POST, "/login").withAuthorization(authorizationRequest).query("fail", "true"))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri!!
                .query("error", "access_denied")
                .query("error_description", UserRejectedRequest.description)
                .query("error_uri", errorUri)
                .query("state", "some state").toString()))
    }

    @Test
    fun `rejects tampered auth request when guard refuses it`() {
        val rejectingValidator = object : AuthoriseRequestValidator {
            override fun isValidClientAndRedirectUriInCaseOfError(
                request: Request,
                clientId: ClientId,
                redirectUri: Uri
            ): Boolean = false

            override fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError> =
                Failure(InvalidRedirectUri)
        }
        val errorRender = AuthoriseRequestErrorRender(
            rejectingValidator,
            RequestJWTValidator.Unsupported,
            JsonResponseErrorRenderer(Jackson, null),
            null
        )
        val underTest = AuthenticationComplete(
            DummyAuthorizationCodes(authorizationRequest, this::isFailure, "jdoe"),
            DummyOAuthAuthRequestTracking(),
            DummyIdTokens("jdoe"),
            null,
            AuthorisationGuard(rejectingValidator, errorRender)
        )

        val response = underTest(Request(POST, "/login").withAuthorization(authorizationRequest))

        assertThat(response, hasStatus(BAD_REQUEST))
        assertThat(response.header("location"), equalTo(null))
        assertThat(response.bodyString().contains("\"code\""), equalTo(false))
    }

    @Test
    fun `guard defaults to AlwaysValid so legacy AuthenticationComplete construction still works`() {
        val response = underTest(Request(POST, "/login").withAuthorization(authorizationRequest))

        assertThat(response, hasStatus(SEE_OTHER))
    }
}

private fun Request.withAuthorization(authorizationRequest: AuthRequest, responseType: ResponseType = Code, responseMode: ResponseMode? = null) =
    with(OAuthServer.clientIdQueryParameter of authorizationRequest.client)
        .with(OAuthServer.scopesQueryParameter of authorizationRequest.scopes)
        .with(OAuthServer.redirectUriQueryParameter of authorizationRequest.redirectUri!!)
        .with(OAuthServer.state of authorizationRequest.state)
        .with(OAuthServer.responseType of responseType)
        .with(OAuthServer.responseMode of responseMode)
