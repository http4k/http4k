package org.http4k.security.oauth.client

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.hamkrest.hasBody
import org.http4k.security.AccessTokenResponse
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.server.OAuthServerMoshi.auto
import org.http4k.util.TickingClock
import org.junit.jupiter.api.Test
import java.time.Duration

class RefreshingOAuthTokenTest {

    @Test
    fun `always gets access token when no refresh token`() {
        val config =
            OAuthProviderConfig(Uri.of("http://auth"), "/authorize", "/oauth/token", Credentials("hello", "world"))

        var counter = 0
        val backend: HttpHandler = {
            Response(OK).with(
                Body.auto<AccessTokenResponse>().toLens() of AccessTokenResponse(
                    it.bodyString() + counter++,
                    "type",
                    100
                )
            )
        }

        val clock = TickingClock()

        val app = ClientFilters.RefreshingOAuthToken(
            config,
            backend,
            { next -> { next(it.body("auth")) } },
            Duration.ofSeconds(10),
            clock
        ).then { req: Request -> Response(OK).body(req.header("Authorization")!!) }

        assertThat(app(Request(GET, "")), hasBody("Bearer auth0"))

        clock.tick(Duration.ofSeconds(88))

        assertThat(app(Request(GET, "")), hasBody("Bearer auth0"))

        clock.tick(Duration.ofSeconds(1))

        assertThat(app(Request(GET, "")), hasBody("Bearer auth1"))
    }

    @Test
    fun `user refresh token when presence`() {
        val config =
            OAuthProviderConfig(Uri.of("http://auth"), "/authorize", "/oauth/token", Credentials("hello", "world"))

        var counter = 0

        val backend: HttpHandler = {
            Response(OK).with(
                Body.auto<AccessTokenResponse>().toLens() of AccessTokenResponse(
                    it.bodyString() + counter++,
                    "type",
                    100,
                    refresh_token = "refresh"
                )
            )
        }

        val clock = TickingClock()

        val app = ClientFilters.RefreshingOAuthToken(
            config,
            backend,
            { next -> { next(it.body("auth")) } },
            Duration.ofSeconds(10),
            clock
        ).then { req: Request -> Response(OK).body(req.header("Authorization")!!) }

        assertThat(app(Request(GET, "")), hasBody("Bearer auth0"))

        clock.tick(Duration.ofSeconds(88))

        assertThat(app(Request(GET, "")), hasBody("Bearer auth0"))

        clock.tick(Duration.ofSeconds(1))

        assertThat(
            app(Request(GET, "")),
            hasBody("Bearer grant_type=refresh_token&client_id=hello&client_secret=world&refresh_token=refresh1")
        )
    }
}
