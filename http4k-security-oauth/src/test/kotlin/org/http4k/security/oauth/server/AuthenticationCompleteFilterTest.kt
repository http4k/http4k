package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.util.*

class AuthenticationCompleteFilterTest {

    private val loginAction = { _: Request -> Response(Status.OK).body("login page") }
    private val persistence = InMemoryOAuthRequestPersistence()

    @Test
    fun `redirects on successful login`() {
        val redirectUri = Uri.of("http://destination")
        persistence.store(AuthorizationRequest(
            UUID.randomUUID(),
            ClientId("a-client-id"),
            listOf("email"),
            redirectUri,
            "some state"
        ), Response(Status.OK))

        val filter = AuthenticationCompleteFilter(
            DummyAuthorizationCodes(),
            persistence
        ).then(loginAction)

        val response = filter(Request(Method.POST, "/login"))

        assertThat(response, hasStatus(Status.TEMPORARY_REDIRECT)
            and hasHeader("location", redirectUri.query("code", "dummy-token").query("state", "some state").toString()))
    }
}