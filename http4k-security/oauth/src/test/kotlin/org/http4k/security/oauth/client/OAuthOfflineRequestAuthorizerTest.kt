package org.http4k.security.oauth.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessTokenResponse
import org.http4k.security.OAuthProviderConfig
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test
import java.lang.UnsupportedOperationException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.*

class OAuthOfflineRequestAuthorizerTest {

    companion object {
        private val request = Request(GET, "/")
        private val accessTokenDuration = Duration.ofSeconds(30)
        private val clientCredentials = Credentials("foo", "bar")
        private const val validRefreshToken = "so_refreshing"
        private val providerConfig = OAuthProviderConfig(
            authBase = Uri.of(""),
            authPath = "/oauth2/authorize",
            tokenPath = "/oauth2/token",
            credentials = clientCredentials
        )
    }

    private var time = Instant.ofEpochSecond(9001)
    private val refreshHistory = mutableListOf<Pair<String, String>>()

    private val authServer = let {
        fun tokenHandler(request: Request): Response {
            val data = OAuthOfflineRequestAuthorizer.tokenRequestLens(request)

            val refreshToken = data.refresh_token ?: return Response(UNAUTHORIZED)
            if (refreshToken != validRefreshToken) return Response(UNAUTHORIZED)

            val responseData = AccessTokenResponse(
                access_token = UUID.randomUUID().toString(),
                expires_in = accessTokenDuration.seconds,
                refresh_token = data.refresh_token,
                token_type = "access_token"
            )
            refreshHistory += refreshToken to responseData.access_token

            return Response(OK)
                .with(OAuthOfflineRequestAuthorizer.tokenResponseLens of responseData)
        }

        ServerFilters.BasicAuth("oauth", clientCredentials)
            .then(routes(providerConfig.tokenPath bind Method.POST to ::tokenHandler))
    }

    private fun client(refreshToken: String, config: OAuthProviderConfig = providerConfig, accessTokenCache: AccessTokenCache = AccessTokenCache.none()): HttpHandler {
        val security = OAuthOfflineRequestAuthorizer(
            config,
            accessTokenCache,
            authServer,
            ClientFilters.BasicAuth(config.credentials),
            FixedClock
        )

        return security.toFilter(refreshToken).then { request ->
            val token = request.header("Authorization")?.replace("Bearer ", "")
            val status = if (token == null) UNAUTHORIZED else OK
            Response(status).body(token ?: "")
        }
    }

    @Test
    fun `call with valid refresh token`() {
        val client = client(validRefreshToken)

        val response = client(request)
        assertThat(response, hasStatus(OK))
        val accessToken = response.bodyString()

        assertThat(refreshHistory, equalTo(listOf(validRefreshToken to accessToken)))
    }

    @Test
    fun `access token will be reused if caching enabled`() {
        val client = client(validRefreshToken, accessTokenCache = AccessTokenCache.inMemory())

        // make first call
        var response = client(request)
        assertThat(response, hasStatus(OK))
        val accessToken = response.bodyString()

        assertThat(refreshHistory, equalTo(listOf(validRefreshToken to accessToken)))

        // make second call (should be same access token as before)
        response = client(request)
        assertThat(response, hasStatus(OK))
        assertThat(response, hasBody(accessToken))
        assertThat(refreshHistory.size, equalTo(1))
    }

    @Test
    fun `subsequent calls without access token cache will request new access token each time`() {
        val client = client(validRefreshToken)

        // make first call
        val response1 = client(request)
        assertThat(response1, hasStatus(OK))


        // make second call (should be new access token)
        val response2 = client(request)
        assertThat(response2, hasStatus(OK))

        assertThat(refreshHistory, equalTo(listOf(
            validRefreshToken to response1.bodyString(),
            validRefreshToken to response2.bodyString()
        )))
    }

    @Test
    fun `call with invalid refresh token - unauthorized`() {
        val client = client("not_refreshing")

        val response = client(request)
        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(refreshHistory, isEmpty)
    }

    @Test
    fun `call with invalid client credentials - unauthorized`() {
        val config = providerConfig.copy(credentials = Credentials("wrong", "credentials"))
        val client = client(validRefreshToken, config = config)

        val response = client(request)
        assertThat(response, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `call with expired cached access token - should be refreshed`() {
        val client = client(validRefreshToken, accessTokenCache = AccessTokenCache.inMemory())

        val response = client(request)
        assertThat(response, hasStatus(OK))

        // expire access token
        time += accessTokenDuration.multipliedBy(2)

        val response2 = client(request)
        assertThat(response2, hasStatus(OK))

        assertThat(refreshHistory, equalTo(listOf(
            validRefreshToken to response.bodyString(),
            validRefreshToken to response2.bodyString()
        )))
    }
}

