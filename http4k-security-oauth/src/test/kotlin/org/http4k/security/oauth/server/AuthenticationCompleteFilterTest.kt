package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.*
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class AuthenticationCompleteFilterTest {

    private val loginAction = { request: Request ->
        if (request.query("fail") == "true")
            Response(UNAUTHORIZED)
        else Response(Status.OK).header("user", "jdoe")
    }

    private val authorizationRequest =
        AuthRequest(
            ClientId("a-client-id"),
            listOf("email"),
            Uri.of("http://destination"),
            "some state"
        )

    val filter = AuthenticationCompleteFilter(
        DummyAuthorizationCodes(authorizationRequest),
        DummyOAuthAuthRequestPersistence()).then(loginAction)

    @Test
    fun `redirects on successful login`() {
        val response = filter(Request(Method.POST, "/login").withAuthorization(authorizationRequest))

        assertThat(response, hasStatus(SEE_OTHER)
            and hasHeader("location",
            authorizationRequest.redirectUri
                .query("code", "dummy-token-for-jdoe")
                .query("state", "some state").toString()))
    }

    @Test
    fun `does not redirect if login is not successful`() {
        val response = filter(Request(Method.POST, "/login").withAuthorization(authorizationRequest).query("fail", "true"))

        assertThat(response, hasStatus(UNAUTHORIZED))
    }
}

private fun Request.withAuthorization(authorizationRequest: AuthRequest) =
    with(OAuthServer.clientId of authorizationRequest.client)
        .with(OAuthServer.scopes of authorizationRequest.scopes)
        .with(OAuthServer.redirectUri of authorizationRequest.redirectUri)
        .with(OAuthServer.state of authorizationRequest.state)