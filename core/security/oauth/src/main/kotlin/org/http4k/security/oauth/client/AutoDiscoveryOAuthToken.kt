package org.http4k.security.oauth.client

import dev.forkhandles.result4k.onFailure
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.filter.ClientFilters
import org.http4k.security.OAuthProviderConfig
import java.time.Clock
import java.time.Duration

fun ClientFilters.AutoDiscoveryOAuthToken(
    authServerDiscovery: AuthServerDiscovery,
    credentials: Credentials,
    backend: HttpHandler,
    clock: Clock = Clock.systemUTC(),
    scopes: List<String> = emptyList(),
    oAuthFlowFilter: Filter = ClientFilters.OAuthClientCredentials(credentials, scopes),
    gracePeriod: Duration = Duration.ofSeconds(10),
): Filter {
    val (authServerUri, metadata) = authServerDiscovery(backend)
        .onFailure { throw it.reason }
    val config = OAuthProviderConfig(
        authBase = authServerUri,
        authPath = metadata.authorization_endpoint.path,
        tokenPath = metadata.token_endpoint.path,
        credentials = credentials
    )

    return ClientFilters.RefreshingOAuthToken(
        config = config,
        backend = backend,
        oAuthFlowFilter = oAuthFlowFilter,
        gracePeriod = gracePeriod,
        clock = clock,
        scopes = scopes
    )
}

