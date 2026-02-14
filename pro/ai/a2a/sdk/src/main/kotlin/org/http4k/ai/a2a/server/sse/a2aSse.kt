package org.http4k.ai.a2a.server.sse

import org.http4k.ai.a2a.server.http.AgentCard
import org.http4k.ai.a2a.server.http.HttpA2aConnection
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.core.then
import org.http4k.filter.CatchAllSse
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.sse.then

/**
 * Creates a PolyHandler for A2A protocol with SSE streaming and HTTP fallback.
 */
fun a2aSse(protocol: A2AProtocol, rpcPath: String = "/", agentCardPath: String = "/.well-known/agent.json") =
    poly(
        ServerFilters.CatchAllSse().then(SseA2aConnection(protocol, rpcPath)),
        CatchAll()
            .then(CatchLensFailure())
            .then(
                routes(
                    AgentCard(agentCardPath, protocol),
                    HttpA2aConnection(protocol, rpcPath)
                )
            )
    )
