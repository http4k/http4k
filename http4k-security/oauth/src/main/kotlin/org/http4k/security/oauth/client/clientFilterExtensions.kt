package org.http4k.security.oauth.client

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
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
import org.http4k.security.AccessTokenExtractor
import org.http4k.security.ContentTypeJsonOrForm
import org.http4k.security.CredentialsProvider
import org.http4k.security.ExpiringCredentials
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.OAuthWebForms.clientId
import org.http4k.security.OAuthWebForms.clientSecret
import org.http4k.security.OAuthWebForms.grantType
import org.http4k.security.OAuthWebForms.password
import org.http4k.security.OAuthWebForms.refreshToken
import org.http4k.security.OAuthWebForms.requestForm
import org.http4k.security.OAuthWebForms.scope
import org.http4k.security.OAuthWebForms.username
import org.http4k.security.Refreshing
import org.http4k.security.oauth.core.RefreshToken
import java.time.Clock
import java.time.Duration
import java.time.Instant.MAX

/**
 * Filter to authenticate and refresh against a OAuth server. Use the correct OAuth filter for your flow.
 * e.g. ClientFilters.ClientCredentials()
 */
fun ClientFilters.RefreshingOAuthToken(
    oauthCredentials: Credentials,
    tokenUri: Uri,
    scopes: List<String> = emptyList(),
    backend: HttpHandler,
    oAuthFlowFilter: Filter = ClientFilters.OAuthClientCredentials(oauthCredentials),
    gracePeriod: Duration = Duration.ofSeconds(10),
    clock: Clock = Clock.systemUTC(),
    tokenExtractor: AccessTokenExtractor = ContentTypeJsonOrForm()
): Filter {
    val refresher = CredentialsProvider.Refreshing<AccessToken>(gracePeriod, clock) {
        val filter = it?.refreshToken
            ?.let { token -> ClientFilters.OAuthRefreshToken(oauthCredentials, token, scopes) }
            ?: oAuthFlowFilter

        filter
            .then(backend)(Request(POST, tokenUri))
            .takeIf { it.status.successful }
            ?.let { tokenExtractor(it).map { it.accessToken }.valueOrNull() }
            ?.let {
                ExpiringCredentials(
                    it, it.expiresIn?.let { clock.instant().plusSeconds(it) } ?: MAX
                )
            }
    }

    return ClientFilters.BearerAuth(CredentialsProvider { refresher()?.value })
}

fun ClientFilters.RefreshingOAuthToken(
    config: OAuthProviderConfig,
    scopes: List<String> = emptyList(),
    backend: HttpHandler,
    oAuthFlowFilter: Filter = ClientFilters.OAuthClientCredentials(config.credentials),
    gracePeriod: Duration = Duration.ofSeconds(10),
    clock: Clock = Clock.systemUTC()
) = ClientFilters.RefreshingOAuthToken(
    config.credentials,
    config.tokenUri,
    scopes,
    backend,
    oAuthFlowFilter,
    gracePeriod,
    clock
)

fun ClientFilters.OAuthUserCredentials(config: OAuthProviderConfig, userCredentials: Credentials) =
    OAuthUserCredentials(config.credentials, userCredentials)

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

fun ClientFilters.OAuthRefreshToken(
    config: OAuthProviderConfig,
    token: RefreshToken,
    scopes: List<String> = emptyList(),
) = OAuthRefreshToken(config.credentials, token, scopes)

fun ClientFilters.OAuthRefreshToken(
    clientCredentials: Credentials,
    token: RefreshToken,
    scopes: List<String> = emptyList(),
) = Filter { next ->
    {
        next(
            it.with(
                if (scopes.isNotEmpty())
                    requestForm of WebForm().with(
                        grantType of "refresh_token",
                        clientId of clientCredentials.user,
                        clientSecret of clientCredentials.password,
                        refreshToken of token,
                        scope of scopes.joinToString(separator = " "),
                    )
                else
                    requestForm of WebForm().with(
                        grantType of "refresh_token",
                        clientId of clientCredentials.user,
                        clientSecret of clientCredentials.password,
                        refreshToken of token,
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
