package org.http4k.security.oauth.client

import dev.forkhandles.result4k.onFailure
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.filter.ClientFilters
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.core.RefreshToken
import java.time.Clock
import java.time.Duration


/**
 * Discovers the authorization server and obtains tokens using the standard client_credentials grant.
 * Suitable for machine-to-machine scenarios with a client ID and secret.
 */
fun ClientFilters.AutoDiscoveryOAuthToken(
    authServerDiscovery: AuthServerDiscovery,
    clientCredentials: Credentials,
    backend: HttpHandler,
    clock: Clock = Clock.systemUTC(),
    scopes: List<String> = emptyList(),
    resourceUri: Uri? = null,
    gracePeriod: Duration = Duration.ofSeconds(10),
) = AutoDiscoveryOAuthToken(
    authServerDiscovery,
    clientCredentials,
    backend,
    ClientFilters.OAuthClientCredentials(clientCredentials, scopes, resourceUri),
    clock,
    scopes,
    resourceUri,
    gracePeriod
)

/**
 * Discovers the authorization server and obtains tokens using a custom initial grant flow
 * but standard client_credentials-based refresh. Use when the initial grant is non-standard
 * (e.g. JWT assertion) but refresh still uses client credentials.
 */
fun ClientFilters.AutoDiscoveryOAuthToken(
    authServerDiscovery: AuthServerDiscovery,
    clientCredentials: Credentials,
    backend: HttpHandler,
    oAuthFlowFilter: Filter,
    clock: Clock = Clock.systemUTC(),
    scopes: List<String> = emptyList(),
    resourceUri: Uri? = null,
    gracePeriod: Duration = Duration.ofSeconds(10),
) = AutoDiscoveryOAuthToken(
    authServerDiscovery,
    backend,
    oAuthFlowFilter,
    { ClientFilters.OAuthRefreshToken(clientCredentials, it, scopes, resourceUri) },
    clock,
    gracePeriod
)

/**
 * Discovers the authorization server and obtains tokens with fully pluggable grant and refresh flows.
 * Use for scenarios where neither the initial grant nor refresh use client credentials (e.g. JWT assertions).
 */
fun ClientFilters.AutoDiscoveryOAuthToken(
    authServerDiscovery: AuthServerDiscovery,
    backend: HttpHandler,
    oAuthFlowFilter: Filter,
    oAuthRefreshFilter: (RefreshToken) -> Filter,
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(10),
): Filter {
    val (authServerUri, metadata) = authServerDiscovery(backend)
        .onFailure { throw it.reason }

    return ClientFilters.RefreshingOAuthToken(
        tokenUri = Uri.of(authServerUri.toString() + metadata.token_endpoint.path),
        backend = backend,
        oAuthFlowFilter = oAuthFlowFilter,
        oAuthRefreshFilter = oAuthRefreshFilter,
        gracePeriod = gracePeriod,
        clock = clock,
    )
}

