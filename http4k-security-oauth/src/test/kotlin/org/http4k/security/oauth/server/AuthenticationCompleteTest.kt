package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.with
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.security.ResponseType
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken
import org.junit.jupiter.api.Test

class AuthenticationCompleteTest {

    private val authorizationRequest =
        AuthRequest(
            ClientId("a-client-id"),
            listOf("email"),
            Uri.of("http://destination"),
            "some state"
        )

    val underTest = AuthenticationComplete(
        DummyAuthorizationCodes(authorizationRequest, this::isFailure, "jdoe"),
        DummyOAuthAuthRequestTracking(),
        DummyIdTokens("jdoe")
    )

    private fun isFailure(request: Request): Boolean = request.query("fail") == "true"

    @Test
    fun `redirects on successful login`() {
        val response = underTest(Request(Method.POST, "/login").withAuthorization(authorizationRequest))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri
                .query("code", "dummy-token-for-jdoe")
                .query("state", "some state").toString()))
    }

    @Test
    fun `includes id_token if response_type requires it`() {
        val response = underTest(Request(Method.POST, "/login").withAuthorization(authorizationRequest, CodeIdToken))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri
                .query("code", "dummy-token-for-jdoe")
                .query("id_token", "dummy-id-token-for-jdoe")
                .query("state", "some state").toString()))
    }

    @Test
    fun `redirects with error details if login is not successful`() {
        val response = underTest(Request(Method.POST, "/login").withAuthorization(authorizationRequest).query("fail", "true"))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri
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
        val response = underTest(Request(Method.POST, "/login").withAuthorization(authorizationRequest).query("fail", "true"))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri
                .query("error", "access_denied")
                .query("error_description", UserRejectedRequest.description)
                .query("error_uri", errorUri)
                .query("state", "some state").toString()))
    }
}

private fun Request.withAuthorization(authorizationRequest: AuthRequest, responseType: ResponseType = Code) =
    with(OAuthServer.clientId of authorizationRequest.client)
        .with(OAuthServer.scopes of authorizationRequest.scopes)
        .with(OAuthServer.redirectUri of authorizationRequest.redirectUri)
        .with(OAuthServer.state of authorizationRequest.state)
        .with(OAuthServer.responseType of responseType)