package org.http4k.security.oauth.client

import dev.forkhandles.result4k.onFailure
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.security.AccessToken
import org.http4k.security.AccessTokenExtractor
import org.http4k.security.ContentTypeJsonOrForm
import org.http4k.security.ExpiringCredentials
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.core.RefreshToken
import java.time.Clock
import java.time.Duration

class OAuthOfflineRequestAuthorizer(
    private val config: OAuthProviderConfig,
    private val accessTokens: AccessTokens,
    backend: HttpHandler,
    authRequestFilter: Filter,
    private val gracePeriod: Duration = Duration.ofSeconds(10),
    private val clock: Clock = Clock.systemUTC(),
    private val accessTokenExtractor: AccessTokenExtractor = ContentTypeJsonOrForm()
) {
    private val authClient = authRequestFilter.then(backend)

    private fun refresh(refreshToken: RefreshToken): ExpiringCredentials<AccessToken>? {
        val body = TokenRequest.refreshToken(refreshToken)

        val request = Request(POST, config.tokenUri)
            .with(tokenRequestLens of body)

        val response = authClient(request)
        if (!response.status.successful) return null

        val accessToken = accessTokenExtractor(response).onFailure { return null }.accessToken
        return ExpiringCredentials(
            accessToken,
            clock.instant().plusSeconds(accessToken.expiresIn ?: Long.MAX_VALUE)
        )
    }

    fun toFilter(refreshToken: RefreshToken) = Filter { next ->
        { request ->
            val tokenData = accessTokens[refreshToken]
                ?.takeIf { Duration.between(clock.instant(), it.expiry) > gracePeriod }
                ?: refresh(refreshToken)?.also { accessTokens[refreshToken] = it }

            val withToken = tokenData?.credentials?.let {
                request.header("Authorization", "${it.type ?: "Bearer"} ${it.value}")
            } ?: request

            next(withToken)
        }
    }
}
