/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.a2a

import dev.forkhandles.result4k.recover
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.client.HttpA2AClient
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.ProtocolVersion
import org.http4k.ai.a2a.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.a2a.capabilities.CancelTask
import org.http4k.ai.mcp.a2a.capabilities.GetTask
import org.http4k.ai.mcp.a2a.capabilities.ListTasks
import org.http4k.ai.mcp.a2a.capabilities.SendMessage
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.capabilities
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import java.security.SecureRandom
import java.util.Random

/**
 * Exposes an A2A agent as a set of MCP tools (`send_message`, `get_task`, `cancel_task`, `list_tasks`),
 * suitable for composing into any http4k MCP server. The agent's card is fetched eagerly so its name,
 * description, and skill catalog can be folded into the `send_message` tool description — fails fast if
 * the card cannot be retrieved.
 */
fun mcpA2aBridge(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    tenant: Tenant? = null,
    protocolVersion: ProtocolVersion = LATEST_VERSION,
    random: Random = SecureRandom()
) = mcpA2aBridge(HttpA2AClient(baseUri, http, tenant, protocolVersion), random)

/**
 * Exposes an A2A agent as a set of MCP tools (`send_message`, `get_task`, `cancel_task`, `list_tasks`),
 * suitable for composing into any http4k MCP server. The agent's card is fetched eagerly so its name,
 * description, and skill catalog can be folded into the `send_message` tool description — fails fast if
 * the card cannot be retrieved.
 */
fun mcpA2aBridge(client: A2AClient, random: Random = SecureRandom()): ServerCapability = capabilities(
    SendMessage(
        client.agentCard().recover { error("Failed to fetch agent card: $it") },
        client, random
    ),
    GetTask(client),
    CancelTask(client),
    ListTasks(client)
)
