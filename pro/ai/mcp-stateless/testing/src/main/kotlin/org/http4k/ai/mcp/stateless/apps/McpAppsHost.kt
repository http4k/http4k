/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.apps

import org.http4k.ai.mcp.stateless.apps.endpoints.ReadUiResource
import org.http4k.ai.mcp.stateless.apps.endpoints.ToolCall
import org.http4k.ai.mcp.stateless.apps.endpoints.Ui
import org.http4k.ai.mcp.stateless.testing.McpClientFactory
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes

/**
 * Basic MCP App host.
 */
fun McpAppsHost(vararg clients: McpClientFactory): RoutingHttpHandler {
    val servers = McpApps(clients.map { it() }).apply { start() }
    return CatchLensFailure()
        .then(
            routes(
                ReadUiResource(servers),
                ToolCall(servers),
                Ui(servers)
            )
        )
}
