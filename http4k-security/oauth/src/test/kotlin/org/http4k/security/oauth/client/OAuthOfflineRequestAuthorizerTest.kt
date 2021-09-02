package org.http4k.security.oauth.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.security.OAuthProviderConfig
import org.junit.jupiter.api.Test
import java.lang.UnsupportedOperationException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class OAuthOfflineRequestAuthorizerTest {

    companion object {
        private val request = Request(GET, "/")
        private val accessTokenDuration = Duration.ofSeconds(30)
    }

    private var time = Instant.ofEpochSecond(9000)

    private val config = OAuthProviderConfig(
        authBase = Uri.of(""),
        authPath = "/oauth2/authorize",
        tokenPath = "/oauth2/token",
        credentials = Credentials("foo", "bar")
    )

    private val authServer = FakeOauthServer(config.tokenPath, accessTokenDuration, "so_refreshing")

    private fun client(refreshToken: String, accessTokenCache: AccessTokenCache = AccessTokenCache.none()): HttpHandler {
        val security = OAuthOfflineRequestAuthorizer(
            config,
            accessTokenCache,
            authServer,
            object: Clock() {
                override fun getZone() = throw UnsupportedOperationException()
                override fun withZone(zone: ZoneId?) = throw UnsupportedOperationException()
                override fun instant() = time
            }
        )

        return security.toFilter(refreshToken).then { request ->
            val token = request.header("Authorization")?.replace("Bearer ", "")
            val status = if (token == null) UNAUTHORIZED else OK
            Response(status).body(token ?: "")
        }
    }

    @Test
    fun `call with valid refresh token`() {
        val client = client("so_refreshing")

        // make first call
        val response = client(request)
        assertThat(response, hasStatus(OK))
        val accessToken = response.bodyString()

        assertThat(authServer.refreshHistory, equalTo(listOf("so_refreshing" to accessToken)))
    }

    @Test
    fun `cached access token will be reused if caching is provided`() {
        val client = client("so_refreshing", accessTokenCache = AccessTokenCache.inMemory())

        // make first call
        var response = client(request)
        assertThat(response, hasStatus(OK))
        val accessToken = response.bodyString()

        assertThat(authServer.refreshHistory, equalTo(listOf("so_refreshing" to accessToken)))

        // make second call (should be same access token as before)
        response = client(request)
        assertThat(response, hasStatus(OK))
        assertThat(response, hasBody(accessToken))
        assertThat(authServer.refreshHistory.size, equalTo(1))
    }

    @Test
    fun `call with invalid refresh token`() {
        val client = client("not_refreshing")

        val response = client(request)
        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(authServer.refreshHistory, isEmpty)
    }

    @Test
    fun `call with expired access token - should be refreshed`() {
        val client = client("so_refreshing")

        val response = client(request)
        assertThat(response, hasStatus(OK))

        // expire access token
        time += accessTokenDuration.multipliedBy(2)

        val response2 = client(request)
        assertThat(response2, hasStatus(OK))

        assertThat(authServer.refreshHistory, equalTo(listOf(
            "so_refreshing" to response.bodyString(),
            "so_refreshing" to response2.bodyString()
        )))
    }
}

