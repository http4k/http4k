package org.http4k.security.oauth.client

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.WebForm
import org.http4k.security.AccessToken
import org.http4k.security.CredentialsProvider
import org.http4k.security.ExpiringCredentials
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.OAuthWebForms.clientId
import org.http4k.security.OAuthWebForms.clientSecret
import org.http4k.security.OAuthWebForms.grantType
import org.http4k.security.OAuthWebForms.refreshToken
import org.http4k.security.OAuthWebForms.requestForm
import org.http4k.security.Refreshing
import org.http4k.security.accessTokenResponseBody
import org.http4k.security.oauth.core.RefreshToken
import java.time.Clock
import java.time.Clock.systemUTC
import java.time.Duration
import java.time.Duration.ofSeconds
import kotlin.Long.Companion.MAX_VALUE

/**
 * Filter to authenticate and refresh against a OAuth server
 */
fun RefreshingOAuthToken(
    config: OAuthProviderConfig,
    backend: HttpHandler,
    authFilter: Filter,
    gracePeriod: Duration = ofSeconds(10),
    clock: Clock = systemUTC()
): Filter {
    fun refreshToken(token: RefreshToken) = Filter { next ->
        {
            next(it.with(requestForm of WebForm()
                .with(
                    grantType of "refresh_token",
                    clientId of config.credentials.user,
                    clientSecret of config.credentials.password,
                    refreshToken of token
                )))
        }
    }

    val refresher = CredentialsProvider.Refreshing<AccessToken>(gracePeriod, clock) {
        val filter = it?.refreshToken?.let(::refreshToken) ?: authFilter

        filter
            .then(backend)(Request(POST, config.tokenUri))
            .takeIf { it.status.successful }
            ?.let { accessTokenResponseBody(it).toAccessToken() }
            ?.let { ExpiringCredentials(it, clock.instant().plusSeconds(it.expiresIn ?: MAX_VALUE)) }
    }

    return ClientFilters.BearerAuth(CredentialsProvider { refresher()?.value })
}
