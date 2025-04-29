package org.http4k.security.oauth.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.filter.ClientFilters
import org.http4k.security.OAuthProviderConfig
import java.time.Clock
import java.time.Duration


fun ClientFilters.AutoDiscoveryOAuthToken(
    authServerUri: Uri,
    credentials: Credentials,
    backend: HttpHandler,
    clock: Clock = Clock.systemUTC(),
    scopes: List<String> = emptyList(),
    oAuthFlowFilter: Filter = ClientFilters.OAuthClientCredentials(credentials, scopes),
    gracePeriod: Duration = Duration.ofSeconds(10),
    authServerDiscovery: AuthServerDiscovery = AuthServerDiscovery.fromKnownAuthServer(authServerUri)
): Filter = when (val discoveryResult = authServerDiscovery(backend)) {
    is Success -> {
        val (authServerUri, metadata) = discoveryResult.value
        val config = OAuthProviderConfig(
            authBase = authServerUri,
            authPath = metadata.authorization_endpoint.path,
            tokenPath = metadata.token_endpoint.path,
            credentials = credentials
        )

        ClientFilters.RefreshingOAuthToken(
            config = config,
            backend = backend,
            oAuthFlowFilter = oAuthFlowFilter,
            gracePeriod = gracePeriod,
            clock = clock,
            scopes = scopes
        )
    }

    is Failure -> ClientFilters.RefreshingOAuthToken(
        config = OAuthProviderConfig(
            authBase = authServerUri,
            authPath = "/authorize",
            tokenPath = "/token",
            credentials = credentials
        ),
        backend = backend,
        oAuthFlowFilter = oAuthFlowFilter,
        gracePeriod = gracePeriod,
        clock = clock,
        scopes = scopes
    )
}

