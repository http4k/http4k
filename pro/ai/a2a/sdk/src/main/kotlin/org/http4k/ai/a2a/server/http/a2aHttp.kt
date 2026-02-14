package org.http4k.ai.a2a.server.http

import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.routes

/**
 * Creates an HTTP handler for A2A protocol endpoints.
 */
fun a2aHttp(protocol: A2AProtocol, rpcPath: String = "/", agentCardPath: String = "/.well-known/agent.json") =
    CatchAll()
        .then(CatchLensFailure())
        .then(
            routes(
                AgentCard(agentCardPath, protocol),
                HttpA2aConnection(protocol, rpcPath)
            )
        )
