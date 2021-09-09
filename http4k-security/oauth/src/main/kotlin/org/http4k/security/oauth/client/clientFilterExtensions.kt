package org.http4k.security.oauth.client

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.core.RefreshToken

fun ClientFilters.oAuthOffline(
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
