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

