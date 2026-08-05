/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server

import org.http4k.ai.mcp.conformance.server.misc.ConformanceMisc
import org.http4k.ai.mcp.conformance.server.prompts.CondormancePrompts
import org.http4k.ai.mcp.conformance.server.resources.ConformanceResources
import org.http4k.ai.mcp.conformance.server.tools.ConformanceTools
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.ServerProtocolCapability
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.http.HttpMcp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.RequestStateCodec
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.server.Helidon
import org.http4k.server.JettyLoom
import org.http4k.server.PolyServerConfig
import org.http4k.server.Undertow
import org.http4k.server.asServer

/**
 * Server which implements the MCP Conformance test suite using the http4k MCP SDK
 */
fun McpConformanceServer(): PolyHandler {
    val metaData = ServerMetaData(
        McpEntity.of("http4k mcp conformance server"), Version.of("0.1.0"),
        *ServerProtocolCapability.entries.toTypedArray()
    )
    val prompts = CondormancePrompts()
    val tools = ConformanceTools(prompts)

    return HttpMcp(
        McpProtocol(
            metaData,
            tools,
            ConformanceResources(),
            prompts,
            ConformanceMisc(),
            requestStateCodec = RequestStateCodec.Hmac("http4k-mcp-conformance".toByteArray()),
        ), NoMcpSecurity,
        corsPolicy = CorsPolicy(
            OriginPolicy.AnyOf("http://localhost:4001"),
            listOf("allowed-header"), listOf(GET, POST, DELETE)
        )
    )
}

// Switch the server backend to probe the handled=false fall-through cross-adapter: -Dmcp.server=undertow|helidon
private fun serverConfig(): PolyServerConfig = when (System.getProperty("mcp.server")?.lowercase()) {
    "undertow" -> Undertow(4001)
    "helidon" -> Helidon(4001)
    else -> JettyLoom(4001)
}

fun main() {
    // NB: no debugMcp() here — its PrintRequestAndResponse reads (consumes) the request body on the SSE path,
    // which drains it before the handled=false fall-through reaches the HTTP face (empty body -> parse error).
    McpConformanceServer().asServer(serverConfig()).start()
}
