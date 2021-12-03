package org.http4k.security.oauth.client

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.lens.WebForm
import org.http4k.security.AccessToken
import org.http4k.security.CredentialsProvider
import org.http4k.security.ExpiringCredentials
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.OAuthWebForms.clientId
import org.http4k.security.OAuthWebForms.clientSecret
import org.http4k.security.OAuthWebForms.grantType
import org.http4k.security.OAuthWebForms.password
import org.http4k.security.OAuthWebForms.refreshToken
import org.http4k.security.OAuthWebForms.requestForm
import org.http4k.security.OAuthWebForms.username
import org.http4k.security.Refreshing
import org.http4k.security.accessTokenResponseBody
import org.http4k.security.oauth.core.RefreshToken
import java.time.Clock
import java.time.Duration
import kotlin.Long.Companion.MAX_VALUE

/**
 * Filter to authenticate and refresh against a OAuth server. Use the correct OAuth filter for your flow.
 * e.g. ClientFilters.ClientCredentials()
 */
fun ClientFilters.RefreshingOAuthToken(
    oauthCredentials: Credentials,
    tokenUri: Uri,
    backend: HttpHandler,
    oAuthFlowFilter: Filter = ClientFilters.OAuthClientCredentials(oauthCredentials),
    gracePeriod: Duration = Duration.ofSeconds(10),
    clock: Clock = Clock.systemUTC()
): Filter {
    val refresher = CredentialsProvider.Refreshing<AccessToken>(gracePeriod, clock) {
        val filter = it?.refreshToken?.let { ClientFilters.OAuthRefreshToken(oauthCredentials, it) } ?: oAuthFlowFilter

        filter
            .then(backend)(Request(POST, tokenUri))
            .takeIf { it.status.successful }
            ?.let { accessTokenResponseBody(it).toAccessToken() }
            ?.let { ExpiringCredentials(it, clock.instant().plusSeconds(it.expiresIn ?: MAX_VALUE)) }
    }

    return ClientFilters.BearerAuth(CredentialsProvider { refresher()?.value })
}

fun ClientFilters.RefreshingOAuthToken(
    config: OAuthProviderConfig,
    backend: HttpHandler,
    oAuthFlowFilter: Filter = ClientFilters.OAuthClientCredentials(config.credentials),
    gracePeriod: Duration = Duration.ofSeconds(10),
    clock: Clock = Clock.systemUTC()
) = ClientFilters.RefreshingOAuthToken(
    config.credentials,
    config.tokenUri,
    backend,
    oAuthFlowFilter,
    gracePeriod,
    clock
)

fun ClientFilters.OAuthUserCredentials(config: OAuthProviderConfig, userCredentials: Credentials) = OAuthUserCredentials(config.credentials, userCredentials)

fun ClientFilters.OAuthUserCredentials(clientCredentials: Credentials, userCredentials: Credentials) = Filter { next ->
    {
        next(
            it.with(
                requestForm of WebForm()
                    .with(
                        grantType of "password",
                        clientId of clientCredentials.user,
                        clientSecret of clientCredentials.password,
                        username of userCredentials.user,
                        password of userCredentials.password,
                    )
            )
        )
    }
}

fun ClientFilters.OAuthClientCredentials(config: OAuthProviderConfig) = OAuthClientCredentials(config.credentials)

fun ClientFilters.OAuthClientCredentials(clientCredentials: Credentials) = Filter { next ->
    {
        next(
            it.with(
                requestForm of WebForm()
                    .with(
                        grantType of "client_credentials",
                        clientId of clientCredentials.user,
                        clientSecret of clientCredentials.password,
                    )
            )
        )
    }
}
fun ClientFilters.OAuthRefreshToken(config: OAuthProviderConfig, token: RefreshToken) = OAuthRefreshToken(config.credentials, token)

fun ClientFilters.OAuthRefreshToken(clientCredentials: Credentials, token: RefreshToken) = Filter { next ->
    {
        next(
            it.with(
                requestForm of WebForm()
                    .with(
                        grantType of "refresh_token",
                        clientId of clientCredentials.user,
                        clientSecret of clientCredentials.password,
                        refreshToken of token
                    )
            )
        )
    }
}

fun ClientFilters.OAuthOffline(
    config: OAuthProviderConfig,
    refreshToken: RefreshToken,
    backend: HttpHandler,
    accessTokens: AccessTokens = AccessTokens.None(),
    authRequestFilter: Filter = BasicAuth(config.credentials)
) = OAuthOfflineRequestAuthorizer(
    config,
    accessTokens,
    backend,
    authRequestFilter
).toFilter(refreshToken)
