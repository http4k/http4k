/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package server.mcp_apps

import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.model.Domain
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.model.apps.Csp
import org.http4k.ai.mcp.stateless.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.stateless.model.apps.McpApps
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.withExtensions
import org.http4k.ai.mcp.stateless.server.capability.extension.McpAppViewModelResourceHandler
import org.http4k.ai.mcp.stateless.server.capability.extension.RenderMcpApp
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.core.PolyHandler
import org.http4k.routing.stateless.bind
import org.http4k.routing.stateless.mcp
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel

class UI : ViewModel

fun UIApp(): PolyHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()

    return mcp(
        ServerMetaData("mcp app", "0.0.0").withExtensions(McpApps),
        NoMcpSecurity,

        RenderMcpApp(
            "show_ui",
            "shows the UI",
            resourceHandler = McpAppViewModelResourceHandler(
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
