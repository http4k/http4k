/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import org.http4k.ai.mcp.protocol.McpExtension
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.McpFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.routing.mcp
import org.http4k.wiretap.Context
import org.http4k.wiretap.junit.RenderMode.OnFailure
import java.security.SecureRandom
import java.time.Clock
import java.util.Random

/**
 * Intercept an http4k MCP ServerCapability. For whole MCP servers, use poly()
 */
fun Intercept.Companion.mcp(
    renderMode: RenderMode = OnFailure,
    redirectFilter: Filter = Filter.NoOp,
    clock: Clock = Clock.systemUTC(),
    random: Random = SecureRandom(byteArrayOf()),
    serverName: String = "http4k-server",
    baseUrl: Uri = Uri.of(""),
    vararg extensions: McpExtension,
    capabilityFn: Context.() -> Iterable<ServerCapability>
) = Intercept.http(
    renderMode,
    redirectFilter,
    clock,
    random,
    serverName,
    baseUrl,
    appFn = {
        ServerFilters.OpenTelemetryTracing(otel())
            .then(
                mcp(
                    ServerMetaData(serverName, "0.0.0").withExtensions(*extensions),
                    NoMcpSecurity,
                    capabilities = capabilityFn().toList().toTypedArray(),
                    mcpFilter = McpFilters.OpenTelemetryTracing(otel())
                )
            ).http!!
    })
