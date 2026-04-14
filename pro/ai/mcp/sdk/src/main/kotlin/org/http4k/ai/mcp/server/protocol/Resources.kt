/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.server.capability.ResourceCapability
import org.http4k.core.Request
import org.http4k.core.Uri

/**
 * Handles protocol traffic for resources features and subscriptions.
 */
interface Resources : ObservableCapability<ResourceCapability>, ResourceHandler, Iterable<ResourceCapability> {

    fun listResources(req: McpResource.List.Request, client: Client, http: Request): McpResource.List.Response

    fun listTemplates(
        req: McpResource.ListTemplates.Request,
        client: Client,
        http: Request
    ): McpResource.ListTemplates.Response

    fun read(req: McpResource.Read.Request, client: Client, http: Request): McpResource.Read.Response
}

