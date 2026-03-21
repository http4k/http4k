/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package wiretap.examples

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Domain
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.Csp
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.capability.extension.RenderMcpApp
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.model.Role
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.McpFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.PolyFilters
import org.http4k.routing.bind
import org.http4k.routing.mcp

fun McpServerWithOtelTracing(client: HttpHandler, otel: OpenTelemetry = GlobalOpenTelemetry.get()): PolyHandler =
    PolyFilters.OpenTelemetryTracing(otel)
        .then(
            mcp(
                ServerMetaData("mcp server with otel", "0.0.0").withExtensions(McpApps),
                NoMcpSecurity,
                RenderMcpApp(
                    name = "show_ui",
                    description = "shows the UI",
                    uri = Uri.of("ui://a-ui"),
                    meta = McpAppResourceMeta(
                        csp = Csp(
                            resourceDomains = listOf(Domain.of("https://resource.com")),
                            connectDomains = listOf(Domain.of("https://connect.com")),
                            frameDomains = listOf(Domain.of("https://frame.com"))
                        )
                    )
                ) {
                    runCatching {
                        ClientFilters.OpenTelemetryTracing(otel).then(client)(
                            Request(GET, "https://http4k.org/")
                        )
                    }

                    "hello world"
                },
                Tool("non_app", "") bind { ToolResponse.Ok("hello") },
                Prompt("prompt", "") bind { PromptResponse(Role.Assistant, "hello") },
                mcpFilter = McpFilters.OpenTelemetryTracing(openTelemetry = otel)
            )
        )

