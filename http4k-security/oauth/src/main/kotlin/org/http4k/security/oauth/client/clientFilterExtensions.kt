package org.http4k.security.oauth.client

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.lens.WebForm
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.OAuthWebForms
import org.http4k.security.OAuthWebForms.clientId
import org.http4k.security.OAuthWebForms.clientSecret
import org.http4k.security.OAuthWebForms.grantType
import org.http4k.security.OAuthWebForms.password
import org.http4k.security.OAuthWebForms.refreshToken
import org.http4k.security.OAuthWebForms.requestForm
import org.http4k.security.OAuthWebForms.username
import org.http4k.security.oauth.core.RefreshToken

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

fun ClientFilters.OAuthUserCredentials(config: OAuthProviderConfig, userCredentials: Credentials) = Filter { next ->
    {
        next(
            it.with(
                requestForm of WebForm()
                    .with(
                        grantType of "password",
                        clientId of config.credentials.user,
                        clientSecret of config.credentials.password,
                        username of userCredentials.user,
                        password of userCredentials.password,
                    )
            )
        )
    }
}

fun ClientFilters.OAuthClientCredentials(config: OAuthProviderConfig) = Filter { next ->
    {
        next(
            it.with(
                requestForm of WebForm()
                    .with(
                        grantType of "client_credentials",
                        clientId of config.credentials.user,
                        clientSecret of config.credentials.password,
                    )
            )
        )
    }
}

fun ClientFilters.OAuthRefreshToken(config: OAuthProviderConfig, token: RefreshToken) = Filter { next ->
    {
        next(
            it.with(
                requestForm of WebForm()
                    .with(
                        grantType of "refresh_token",
                        clientId of config.credentials.user,
                        clientSecret of config.credentials.password,
                        refreshToken of token
                    )
            )
        )
    }
}
