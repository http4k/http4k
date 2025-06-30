package org.http4k.ai.mcp.client

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.NoOp
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.WwwAuthenticate
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header.WWW_AUTHENTICATE
import org.http4k.security.oauth.client.AuthServerDiscovery.Companion.fromKnownAuthServer
import org.http4k.security.oauth.client.AuthServerDiscovery.Companion.fromProtectedResource
import org.http4k.security.oauth.client.AutoDiscoveryOAuthToken
import java.time.Clock

/**
 * MCP OAuth Client filter that handles the authentication process for MCP resources.
 * It expects the server to provide the authorization server URL or protected resource in the WWW-Authenticate header.
 * It then retrieves the token using the provided client credentials and retries the request.
 */
fun ClientFilters.DiscoveredMcpOAuth(
    clientCredentials: Credentials,
    scopes: List<String> = emptyList(),
    clock: Clock = Clock.systemUTC()
) = object : Filter {
    private var auth = Filter.NoOp

    override fun invoke(next: HttpHandler): HttpHandler {
        return {
            val response = auth.then(next)(it)
            when {
                response.status == UNAUTHORIZED ->
                    when (val wwwAuthenticate = WWW_AUTHENTICATE(response)) {
                        null -> response

                        else -> when {
                            wwwAuthenticate["resource_metadata"] != null -> {
                                auth = authFromProtectedResource(wwwAuthenticate, next, it.uri)
                                auth.then(next)(it)
                            }

                            wwwAuthenticate["auth_server"] != null -> {
                                auth = authFromAuthServer(wwwAuthenticate, next)
                                auth.then(next)(it)
                            }

                            else -> response
                        }
                    }

                else -> response
            }
        }
    }

    private fun authFromAuthServer(wwwAuthenticate: WwwAuthenticate, next: HttpHandler) =
        ClientFilters.AutoDiscoveryOAuthToken(
            fromKnownAuthServer(Uri.of(wwwAuthenticate["auth_server"]!!)),
            clientCredentials,
            next
        )

    private fun authFromProtectedResource(
        wwwAuthenticate: WwwAuthenticate,
        next: HttpHandler,
        resourceUri: Uri,
    ): Filter {
        val resourceMetadataUri = Uri.of(wwwAuthenticate["resource_metadata"]!!)
        val resourceMetadataUriWithSchema = if (resourceMetadataUri.scheme == "") resourceUri.path(resourceMetadataUri.path) else resourceMetadataUri

        return ClientFilters.AutoDiscoveryOAuthToken(
            fromProtectedResource(resourceMetadataUriWithSchema),
            clientCredentials,
            next,
            clock,
            scopes,
            resourceUri
        )
    }
}
