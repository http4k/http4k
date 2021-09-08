package org.http4k.security.oauth.client

import org.http4k.core.HttpHandler
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.core.RefreshToken

fun ClientFilters.oAuthOffline(
    config: OAuthProviderConfig,
    refreshToken: RefreshToken,
    backend: HttpHandler,
    accessTokenCache: AccessTokenCache = AccessTokenCache.none(),
) = OAuthOfflineRequestAuthorizer(
    config,
    accessTokenCache,
    backend,
    authRequestFilter = BasicAuth(config.credentials)
).toFilter(refreshToken)

fun ClientFilters.oAuthOffline(
    config: OAuthProviderConfig,
    refreshToken: String,
    backend: HttpHandler,
    accessTokenCache: AccessTokenCache = AccessTokenCache.none(),
) = OAuthOfflineRequestAuthorizer(
    config,
    accessTokenCache,
    backend,
    authRequestFilter = BasicAuth(config.credentials)
).toFilter(refreshToken)
