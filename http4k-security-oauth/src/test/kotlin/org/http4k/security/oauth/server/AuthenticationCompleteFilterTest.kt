package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test

class AuthenticationCompleteFilterTest {

    private val loginAction = { request: Request ->
        if (request.query("fail") == "true")
            Response(UNAUTHORIZED)
        else Response(Status.OK)
    }

    private val authorizationRequest =
        AuthorizationRequest(
            ClientId("a-client-id"),
            listOf("email"),
            Uri.of("http://destination"),
            "some state"
        )

    val filter = AuthenticationCompleteFilter(
        DummyAuthorizationCodes(authorizationRequest),
        ClientValidationFilter(HardcodedClientValidator(authorizationRequest.client, authorizationRequest.redirectUri)), FixedClock).then(loginAction)

    @Test
    fun `redirects on successful login`() {
        val response = filter(Request(Method.POST, "/login").withAuthorization(authorizationRequest))

        assertThat(response, hasStatus(TEMPORARY_REDIRECT)
            and hasHeader("location",
            authorizationRequest.redirectUri
                .query("code", "dummy-token")
                .query("state", "some state").toString()))
    }

    @Test
    fun `does not redirect if login is not successful`() {
        val response = filter(Request(Method.POST, "/login").withAuthorization(authorizationRequest).query("fail", "true"))

        assertThat(response, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `validates client_id and redirect_uri values`() {
        val invalidAuthorizationRequest = authorizationRequest
            .copy(client = ClientId("invalid"), redirectUri = Uri.of("http://invalid"))

        val response = filter(Request(Method.GET, "/login").withAuthorization(invalidAuthorizationRequest))
        assertThat(response, hasStatus(Status.BAD_REQUEST))
        assertThat(response.status.description, equalTo("invalid 'client_id' and/or 'redirect_uri'"))
    }
}

private fun Request.withAuthorization(authorizationRequest: AuthorizationRequest) =
    with(OAuthServer.clientId of authorizationRequest.client)
        .with(OAuthServer.scopes of authorizationRequest.scopes)
        .with(OAuthServer.redirectUri of authorizationRequest.redirectUri)
        .with(OAuthServer.state of authorizationRequest.state)