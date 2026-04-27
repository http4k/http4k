/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentSkill
import org.http4k.ai.a2a.server.capability.MessageCapability
import org.http4k.ai.a2a.server.capability.ServerCapability
import org.http4k.ai.a2a.server.capability.messages
import org.http4k.ai.a2a.server.http.AgentCard
import org.http4k.ai.a2a.server.http.HttpA2aConnection
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.ai.a2a.server.security.A2ASecurity
import org.http4k.ai.a2a.server.security.NoA2ASecurity
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure

infix fun AgentSkill.bind(handler: MessageHandler) = MessageCapability(listOf(this), handler)

infix fun List<AgentSkill>.bind(handler: MessageHandler) = MessageCapability(this, handler)

/**
 * Create an A2A server. Handles both single JSON-RPC responses and SSE streaming
 * based on the request method (message/send vs message/stream).
 */
fun a2a(
    agentCard: AgentCard,
    security: A2ASecurity = NoA2ASecurity,
    vararg capabilities: ServerCapability,
    rpcPath: String = "/",
    agentCardPath: String = "/.well-known/agent.json"
): HttpHandler {
    val protocol = protocolFrom(agentCard, capabilities)
    return security.filter
        .then(CatchAll())
        .then(CatchLensFailure())
        .then(
            routes(
                AgentCard(agentCardPath, protocol),
                HttpA2aConnection(protocol, rpcPath)
            )
        )
}

private fun protocolFrom(
    agentCard: AgentCard,
    capabilities: Array<out ServerCapability>
): A2AProtocol {
    val messageCapability = capabilities.flatMap { it }
        .filterIsInstance<MessageCapability>()
        .firstOrNull()

    return A2AProtocol(
        agentCard,
        messages(messageCapability?.handler ?: error("No MessageCapability provided"))
    )
}
