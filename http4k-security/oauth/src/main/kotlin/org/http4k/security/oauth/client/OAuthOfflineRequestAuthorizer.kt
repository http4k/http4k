package org.http4k.security.oauth.client

import org.http4k.core.*
import org.http4k.filter.ClientFilters
import org.http4k.security.OAuthProviderConfig
import org.http4k.format.Moshi.auto
import org.http4k.security.AccessTokenResponse
import org.http4k.security.oauth.core.RefreshToken
import java.time.Clock
import java.time.Duration

class OAuthOfflineRequestAuthorizer(
    private val config: OAuthProviderConfig,
    private val accessTokenCache: AccessTokenCache,
    backend: HttpHandler,
    private val clock: Clock = Clock.systemUTC(),
    private val expiryMargin: Duration = Duration.ofSeconds(10)
) {
    companion object {
        val tokenRequestLens = Body.auto<TokenRequest>().toLens()
        val tokenResponseLens = Body.auto<AccessTokenResponse>().toLens()
    }

    private val authClient = ClientFilters.SetHostFrom(config.apiBase)
        .then(ClientFilters.BasicAuth(config.credentials))
        .then(backend)

    private fun refresh(refreshToken: RefreshToken): TokenData? {
        val body = TokenRequest.refreshToken(refreshToken)

        val request = Request(Method.POST, config.tokenUri)
            .with(tokenRequestLens of body)

        val response = authClient(request)
        if (!response.status.successful) return null

        val responseBody = tokenResponseLens(response)
        return TokenData(
            accessToken = responseBody.access_token,
            expiresAt = responseBody.expires_in?.let { clock.instant().plusSeconds(it) }
        )
    }

    fun toFilter(refreshToken: RefreshToken) = Filter { next ->
        { request ->
            val tokenData = accessTokenCache[refreshToken]
                ?.takeIf { Duration.between(clock.instant(), it.expiresAt) > expiryMargin }
                ?: refresh(refreshToken)?.also { accessTokenCache[refreshToken] = it }

            val withToken = tokenData?.let {
                request.header("Authorization", "Bearer ${tokenData.accessToken}")
            } ?: request

            next(withToken)
        }
    }

    fun toFilter(refreshToken: String) = toFilter(RefreshToken(refreshToken))
 }
