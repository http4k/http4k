package org.http4k.mcp.server.security

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.filter.ServerFilters
import org.http4k.lens.Lens
import org.http4k.routing.RoutingHttpHandler
import org.http4k.security.Security
import org.http4k.security.oauth.metadata.ResourceMetadata
import org.http4k.security.oauth.server.BearerAuthWithAuthServerDiscovery
import org.http4k.security.oauth.server.OAuthProtectedResourceMetadata

/**
 * Provides a way to secure an MCP server using various authentication methods.
 */
interface McpSecurity : Security {
    val routes: List<RoutingHttpHandler>

    companion object {
        object None : McpSecurity {
            override val routes = emptyList<RoutingHttpHandler>()
            override val filter = Filter.NoOp
        }

        fun BasicAuth(realm: String, credentials: (Credentials) -> Boolean) = object : McpSecurity {
            override val routes = emptyList<RoutingHttpHandler>()
            override val filter = ServerFilters.BasicAuth(realm, credentials)
        }

        fun <T> ApiKey(
            lens: (Lens<Request, T>),
            validate: (T) -> Boolean
        ) = object : McpSecurity {
            override val routes = emptyList<RoutingHttpHandler>()
            override val filter = ServerFilters.ApiKeyAuth(lens, validate)
        }

        fun BearerAuth(checkToken: (String) -> Boolean) = object : McpSecurity {
            override val routes = emptyList<RoutingHttpHandler>()
            override val filter = ServerFilters.BearerAuth(checkToken)
        }

        fun BearerAuthWithAuthServerDiscovery(
            resourceMetadata: ResourceMetadata,
            vararg contents: Pair<String, String>,
            checkToken: (String) -> Boolean
        ) = object : McpSecurity {
            override val filter =
                ServerFilters.BearerAuthWithAuthServerDiscovery(
                    resourceMetadata.authorizationServers?.first()
                        ?: error("No Authorization Server defined"),
                    *contents,
                    checkToken = checkToken
                )

            override val routes = listOf(OAuthProtectedResourceMetadata(resourceMetadata))
        }
    }
}
