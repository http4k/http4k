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
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.LensInjectorExtractor
import java.security.SecureRandom
import java.util.Random

/**
 * Exposes an A2A agent as a set of MCP tools (`send_message`, `get_task`, `cancel_task`, `list_tasks`),
 * suitable for composing into any http4k MCP server. The agent's card is fetched eagerly (with an empty
 * request, so any caller-provided auth wiring runs with no `Authorization` header) so the skill catalog
 * can be folded into the `send_message` tool description — fails fast if the card cannot be retrieved.
 *
 * `clientFor` is invoked per tool call with the inbound MCP HTTP request, so the caller controls how
 * each per-call A2A client is built (e.g. forwarding the caller's `Authorization` header).
 */
fun mcpA2aBridge(clientFor: (Request) -> A2AClient, random: Random = SecureRandom()): ServerCapability {
    val card = clientFor(Request(GET, "")).agentCard()
        .recover { error("Failed to fetch agent card: $it") }

    return capabilities(
        SendMessage(card, clientFor, random),
        GetTask(clientFor),
        CancelTask(clientFor),
        ListTasks(clientFor)
    )
}

/**
 * Convenience overload for a fixed pre-built [A2AClient] — every tool call goes out under the same
 * credentials baked into the client. Use the [clientFor] form if you need per-call auth.
 */
fun mcpA2aBridge(client: A2AClient, random: Random = SecureRandom()): ServerCapability =
    mcpA2aBridge({ client }, random)

/**
 * Convenience overload that forwards the inbound MCP request's `Authorization` header to the A2A
 * agent. The agent card is fetched without auth (typically `.well-known/` is public).
 */
fun <T> mcpA2aBridge(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    tenant: Tenant? = null,
    protocolVersion: ProtocolVersion = LATEST_VERSION,
    random: Random = SecureRandom(),
    authLens: LensInjectorExtractor<Request, T?>
): ServerCapability = mcpA2aBridge({ request ->
    val forwardAuth = Filter { next -> { next(it.with(authLens of authLens(request))) } }
    HttpA2AClient(baseUri, http = forwardAuth.then(http), tenant, protocolVersion)
}, random)
