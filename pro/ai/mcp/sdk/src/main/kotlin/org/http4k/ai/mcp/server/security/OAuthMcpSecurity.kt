package org.http4k.ai.mcp.server.security

import org.http4k.core.Uri
import org.http4k.filter.ServerFilters
import org.http4k.security.oauth.metadata.ResourceMetadata
import org.http4k.security.oauth.server.BearerAuthWithResourceMetadata
import org.http4k.security.oauth.server.OAuthProtectedResourceMetadata

/**
 * Standard OAuthSecurity implementation for MCP. Based around the OAuth protected resource metadata endpoint.
 * You can pass in specific contents of the ResourceMetadata, or extra fields to be added to the WWW-Authenticate header.
 */
class OAuthMcpSecurity(
    resourceMetadata: ResourceMetadata,
    vararg extraWwwAuthenticateFields: Pair<String, String>,
    private val checkToken: (String) -> Boolean
) : McpSecurity {

    override val name = "OAuth"

    /**
     * Bare bones MCP Security implementation that uses the OAuth protected resource metadata endpoint
     */
    constructor(authorizationServerUri: Uri, mcpUri: Uri, checkToken: (String) -> Boolean) :
        this(ResourceMetadata(mcpUri, listOf(authorizationServerUri)), checkToken = checkToken)

    override val filter = ServerFilters.BearerAuthWithResourceMetadata(
        Uri.of(".well-known/oauth-protected-resource"),
        *extraWwwAuthenticateFields,
        checkToken = checkToken
    )

    override val routes = listOf(OAuthProtectedResourceMetadata(resourceMetadata))
}
