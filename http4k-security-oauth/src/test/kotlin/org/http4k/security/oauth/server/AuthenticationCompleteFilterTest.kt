package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.util.*

class AuthenticationCompleteFilterTest {

    private val loginAction = { request: Request ->
        if (request.query("fail") == "true")
            Response(UNAUTHORIZED)
        else Response(Status.OK)
    }

    private val persistence = InMemoryOAuthRequestPersistence()

    private val authorizationRequest =
        AuthorizationRequest(
            UUID.randomUUID(),
            ClientId("a-client-id"),
            listOf("email"),
            Uri.of("http://destination"),
            "some state"
        )

    val filter = AuthenticationCompleteFilter(
        DummyAuthorizationCodes(),
        persistence
    ).then(loginAction)

    @Test
    fun `redirects on successful login`() {
        persistence.store(authorizationRequest, Response(OK))

        val response = filter(Request(Method.POST, "/login"))

        assertThat(response, hasStatus(TEMPORARY_REDIRECT)
            and hasHeader("location",
            authorizationRequest.redirectUri
                .query("code", "dummy-token")
                .query("state", "some state").toString()))
        assertThat(persistence.isEmpty, equalTo(true))
    }

    @Test
    fun `does not redirect if login is not successful`() {
        persistence.store(authorizationRequest, Response(Status.OK))

        val response = filter(Request(Method.POST, "/login").query("fail", "true"))

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(persistence.isEmpty, equalTo(true))
    }
}