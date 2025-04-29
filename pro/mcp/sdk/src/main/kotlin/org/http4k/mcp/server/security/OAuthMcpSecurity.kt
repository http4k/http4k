package org.http4k.mcp.server.security

import org.http4k.core.Uri
import org.http4k.filter.ServerFilters
import org.http4k.security.oauth.metadata.ResourceMetadata
import org.http4k.security.oauth.server.BearerAuthWithResourceMetadata
import org.http4k.security.oauth.server.OAuthProtectedResourceMetadata

/**
 * Standard OAuthSecurity implementation for MCP. Based around the OAuth protected resource metadata endpoint.
 */
class OAuthMcpSecurity(
    resourceMetadata: ResourceMetadata,
    vararg contents: Pair<String, String>,
    mcpPath: String = "/mcp",
    private val checkToken: (String) -> Boolean
) : McpSecurity {
    /**
     * Bare bones MCP Security implementation that uses the OAuth protected resource metadata endpoint
     */
    constructor(authorizationServerUri: Uri, mcpPath: String = "/mcp", checkToken: (String) -> Boolean) :
            this(ResourceMetadata(Uri.of(mcpPath), listOf(authorizationServerUri)), checkToken = checkToken)

    override val filter = ServerFilters.BearerAuthWithResourceMetadata(
        Uri.of(".well-known/oauth-protected-resource/$mcpPath"),
        *contents,
        checkToken = checkToken
    )

    override val routes = listOf(OAuthProtectedResourceMetadata(resourceMetadata))
}
