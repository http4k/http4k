/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.ProtocolVersion
import org.http4k.ai.a2a.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.a2a.mcpA2aBridge
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.server.protocol.NoOp
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.lens.Header
import java.security.SecureRandom
import java.util.Random

/**
 * A complete MCP Server that bridges an A2A agent at [baseUri] — the agent card is fetched once
 * at construction (typically from `.well-known/agent-card.json`) and the resulting tools (`send_message`,
 * `get_task`, `cancel_task`, `list_tasks`) are exposed under [path].
 *
 * The inbound MCP request's `Authorization` header is forwarded to the A2A agent on every tool call.
 * For other passthrough schemes (e.g. `X-Api-Key`), use [mcpA2aBridge] with a custom lens and wrap it
 * with [mcp].
 */
fun mcpA2aBridgeServer(
    identity: ServerMetaData,
    baseUri: Uri,
    security: McpSecurity,
    tenant: Tenant? = null,
    http: HttpHandler = JavaHttpClient(responseBodyMode = BodyMode.Stream),
    mcpFilter: McpFilter = McpFilter.NoOp,
    a2aProtocolVersion: ProtocolVersion = LATEST_VERSION,
    random: Random = SecureRandom(),
    path: String = "/mcp"
) = mcp(
    identity,
    security,
    mcpA2aBridge(baseUri, http, tenant, a2aProtocolVersion, random, Header.optional("Authorization")),
    mcpFilter = mcpFilter,
    path = path
)
