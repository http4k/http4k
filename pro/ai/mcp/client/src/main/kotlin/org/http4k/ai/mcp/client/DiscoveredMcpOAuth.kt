/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
import org.http4k.security.oauth.client.OAuthClientCredentials
import org.http4k.security.oauth.client.OAuthRefreshToken
import org.http4k.security.oauth.client.AuthServerDiscovery.Companion.fromKnownAuthServer
import org.http4k.security.oauth.core.RefreshToken
import org.http4k.security.oauth.client.AuthServerDiscovery.Companion.fromProtectedResource
import org.http4k.security.oauth.client.AutoDiscoveryOAuthToken
import java.time.Clock

/**
 * MCP OAuth Client filter using the standard client_credentials grant. Suitable for machine-to-machine
 * scenarios where the client has a client ID and secret. Discovers the authorization server automatically
 * from the MCP resource server's WWW-Authenticate header or .well-known endpoint (SEP-985 fallback).
 */
fun ClientFilters.DiscoveredMcpOAuth(
    clientCredentials: Credentials,
    scopes: List<String> = emptyList(),
    clock: Clock = Clock.systemUTC()
) = DiscoveredMcpOAuth(
    clientCredentials,
    ClientFilters.OAuthClientCredentials(clientCredentials, scopes),
    clock
)

/**
 * MCP OAuth Client filter with a custom initial grant flow but standard client_credentials-based refresh.
 * Use when the initial token acquisition uses a non-standard grant (e.g. JWT assertion for ID-JAG)
 * but the client still has credentials for refreshing tokens.
 */
fun ClientFilters.DiscoveredMcpOAuth(
    clientCredentials: Credentials,
    oAuthFlowFilter: Filter,
    clock: Clock = Clock.systemUTC()
) = DiscoveredMcpOAuth(
    oAuthFlowFilter,
    { ClientFilters.OAuthRefreshToken(clientCredentials, it) },
    clock
)

/**
 * MCP OAuth Client filter with fully pluggable grant and refresh flows. Use for enterprise auth scenarios
 * where neither the initial grant nor the refresh use client credentials.
 * Both the initial token acquisition and token refresh are controlled by the provided filters.
 */
fun ClientFilters.DiscoveredMcpOAuth(
    oAuthFlowFilter: Filter,
    oAuthRefreshFilter: (RefreshToken) -> Filter,
    clock: Clock = Clock.systemUTC()
) = object : Filter {
    private var auth = Filter.NoOp

    override fun invoke(next: HttpHandler): HttpHandler {
        return {
            val response = auth.then(next)(it)
            when {
                response.status == UNAUTHORIZED ->
                    when (val wwwAuthenticate = WWW_AUTHENTICATE(response)) {
                        null -> {
                            auth = authFromWellKnown(next, it.uri)
                            auth.then(next)(it)
                        }

                        else -> when {
                            wwwAuthenticate["resource_metadata"] != null -> {
                                auth = authFromProtectedResource(wwwAuthenticate, next, it.uri)
                                auth.then(next)(it)
                            }

                            wwwAuthenticate["auth_server"] != null -> {
                                auth = authFromAuthServer(wwwAuthenticate, next)
                                auth.then(next)(it)
                            }

                            else -> {
                                auth = authFromWellKnown(next, it.uri)
                                auth.then(next)(it)
                            }
                        }
                    }

                else -> response
            }
        }
    }

    private fun authFromAuthServer(wwwAuthenticate: WwwAuthenticate, next: HttpHandler) =
        ClientFilters.AutoDiscoveryOAuthToken(
            fromKnownAuthServer(Uri.of(wwwAuthenticate["auth_server"]!!)),
            next,
            oAuthFlowFilter,
            oAuthRefreshFilter,
            clock,
        )

    private fun authFromProtectedResource(
        wwwAuthenticate: WwwAuthenticate,
        next: HttpHandler,
        resourceUri: Uri,
    ): Filter {
        val resourceMetadataUri = Uri.of(wwwAuthenticate["resource_metadata"]!!)
        val resourceMetadataUriWithSchema = if (resourceMetadataUri.scheme == "") resourceUri.path(resourceMetadataUri.path) else resourceMetadataUri

        return ClientFilters.AutoDiscoveryOAuthToken(
            fromProtectedResource(resourceMetadataUriWithSchema, resourceUri),
            next,
            oAuthFlowFilter,
            oAuthRefreshFilter,
            clock,
        )
    }

    private fun authFromWellKnown(next: HttpHandler, resourceUri: Uri) =
        ClientFilters.AutoDiscoveryOAuthToken(
            fromProtectedResource(resourceUri.path("/.well-known/oauth-protected-resource"), resourceUri),
            next,
            oAuthFlowFilter,
            oAuthRefreshFilter,
            clock,
        )
}
