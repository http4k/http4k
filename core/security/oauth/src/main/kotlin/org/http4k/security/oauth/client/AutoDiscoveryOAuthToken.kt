package org.http4k.security.oauth.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.filter.ClientFilters
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.format.OAuthMoshi
import org.http4k.security.oauth.metadata.ServerMetadata
import java.time.Clock
import java.time.Duration

fun ClientFilters.AutoDiscoveryOAuthToken(
    serverUri: Uri,
    credentials: Credentials,
    backend: HttpHandler,
    clock: Clock = Clock.systemUTC(),
    oAuthFlowFilter: Filter = ClientFilters.OAuthClientCredentials(credentials, emptyList()),
    scopes: List<String> = emptyList(),
    gracePeriod: Duration = Duration.ofSeconds(10)
) = when (val discoveryResult = backend.discoverEndpoints(serverUri)) {
    is Success -> {
        val metadata = discoveryResult.value
        val config = OAuthProviderConfig(
            authBase = serverUri,
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
            authBase = serverUri,
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

private fun HttpHandler.discoverEndpoints(baseUri: Uri): Result<ServerMetadata, Exception> {
    val response = this(Request(GET, baseUri.path("/.well-known/oauth-authorization-server")))

    return when {
        response.status.successful -> Success(OAuthMoshi.asA(response.bodyString(), ServerMetadata::class))
        else -> Failure(Exception("Failed to discover OAuth endpoints"))
    }
}
