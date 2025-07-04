package org.http4k.ai.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.messages.McpResource

/**
 * Handles protocol traffic for resources features and subscriptions.
 */
interface Resources {

    fun listResources(req: McpResource.List.Request, client: Client, http: Request): McpResource.List.Response

    fun listTemplates(
        req: McpResource.ListTemplates.Request,
        client: Client,
        http: Request
    ): McpResource.ListTemplates.Response

    fun read(req: McpResource.Read.Request, client: Client, http: Request): McpResource.Read.Response
}

