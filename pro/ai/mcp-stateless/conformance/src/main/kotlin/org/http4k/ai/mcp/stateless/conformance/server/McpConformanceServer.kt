/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server

import org.http4k.ai.mcp.stateless.conformance.server.misc.ConformanceMisc
import org.http4k.ai.mcp.stateless.conformance.server.prompts.CondormancePrompts
import org.http4k.ai.mcp.stateless.conformance.server.resources.ConformanceResources
import org.http4k.ai.mcp.stateless.conformance.server.tools.ConformanceTools
import org.http4k.ai.mcp.stateless.model.McpEntity
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.ServerProtocolCapability
import org.http4k.ai.mcp.stateless.protocol.Version
import org.http4k.ai.mcp.stateless.server.extension.McpTasks
import org.http4k.ai.mcp.stateless.server.http.HttpMcp
import org.http4k.ai.mcp.stateless.server.protocol.McpProtocol
import org.http4k.ai.mcp.stateless.server.protocol.RequestStateCodec
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.server.Helidon
import org.http4k.server.asServer

/**
 * Server which implements the MCP Conformance test suite using the http4k MCP SDK
 */
fun McpConformanceServer(): PolyHandler {
    val tasks = McpTasks()

    return HttpMcp(
        McpProtocol(
            ServerMetaData(
                McpEntity.of("http4k mcp conformance server"), Version.of("0.1.0"),
                *ServerProtocolCapability.entries.toTypedArray<ServerProtocolCapability>()
            ),
            ConformanceTools(CondormancePrompts(), tasks),
            ConformanceResources(),
            CondormancePrompts(),
            ConformanceMisc(),
            requestStateCodec = RequestStateCodec.Hmac("http4k-mcp-conformance".toByteArray()),
            extensions = listOf(tasks),
        ), NoMcpSecurity,
        corsPolicy = CorsPolicy(
            OriginPolicy.AnyOf("http://localhost:4001"),
            listOf("allowed-header"), listOf(GET, POST, DELETE)
        )
    )
}

fun main() {
    McpConformanceServer().asServer(Helidon(4001)).start()
}
