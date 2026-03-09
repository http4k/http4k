/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.mcp.apps.Apps
import org.http4k.wiretap.mcp.prompts.Prompts
import org.http4k.wiretap.mcp.resources.Resources
import org.http4k.wiretap.mcp.tools.Tools

fun InboundClient(mcpClient: McpClient) = object : WiretapFunction {

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer): RoutingHttpHandler {
        val mcpApps = McpApps(listOf(mcpClient)).apply { runCatching { start() } }

        val functions = listOf(Tools(mcpClient), Prompts(mcpClient), Resources(mcpClient), Apps(mcpApps))

        return routes(
            *functions.map { it.http(elements, html) }.toTypedArray(),
            Index(html, Index),
        )
    }

    override fun mcp() = CapabilityPack()
}
