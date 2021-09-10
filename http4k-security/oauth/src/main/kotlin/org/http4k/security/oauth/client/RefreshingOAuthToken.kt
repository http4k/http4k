package org.http4k.security.oauth.client

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.security.AccessToken
import org.http4k.security.CredentialsProvider
import org.http4k.security.ExpiringCredentials
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.Refreshing
import org.http4k.security.accessTokenResponseBody
import java.time.Clock
import java.time.Clock.systemUTC
import java.time.Duration
import java.time.Duration.ofSeconds
import kotlin.Long.Companion.MAX_VALUE

/**
 * Filter to authenticate and refresh against a OAuth server. Use the correct OAuth filter for your flow.
 * e.g. ClientFilters.ClientCredentials()
 */
fun RefreshingOAuthToken(
    config: OAuthProviderConfig,
    backend: HttpHandler,
    oAuthFlowFilter: Filter = ClientFilters.OAuthClientCredentials(config),
    gracePeriod: Duration = ofSeconds(10),
    clock: Clock = systemUTC()
): Filter {
    val refresher = CredentialsProvider.Refreshing<AccessToken>(gracePeriod, clock) {
        val filter = it?.refreshToken?.let { ClientFilters.OAuthRefreshToken(config, it) } ?: oAuthFlowFilter

        filter
            .then(backend)(Request(POST, config.tokenUri))
            .takeIf { it.status.successful }
            ?.let { accessTokenResponseBody(it).toAccessToken() }
            ?.let { ExpiringCredentials(it, clock.instant().plusSeconds(it.expiresIn ?: MAX_VALUE)) }
    }

    return ClientFilters.BearerAuth(CredentialsProvider { refresher()?.value })
}
