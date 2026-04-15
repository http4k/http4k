/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package server.mcp_apps

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Domain
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.Csp
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.capability.extension.McpAppViewModelResourceHandler
import org.http4k.ai.mcp.server.capability.extension.RenderMcpApp
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel

class UI() : ViewModel

fun UIApp(): PolyHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()

    return mcp(
        ServerMetaData("mcp app", "0.0.0").withExtensions(McpApps),
        NoMcpSecurity,

        RenderMcpApp(
            "show_ui",
            "shows the UI",
            resourceHandler = McpAppViewModelResourceHandler(
                Uri.of("ui://a-ui"),
                renderer,
                McpAppResourceMeta(
                    csp = Csp(
                        resourceDomains = listOf(Domain.of("https://resource.com")),
                        connectDomains = listOf(Domain.of("https://connect.com")),
                        frameDomains = listOf(Domain.of("https://frame.com"))
                    )
                ),
            ) { UI() }

        ),
        Tool("standard_tool", "") bind { Ok("hello") },
    )
}
