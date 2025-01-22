package org.http4k.mcp.server

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.jsonRpcRequest
import org.http4k.lens.Query
import org.http4k.lens.value
import org.http4k.mcp.util.McpJson
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.SseMessage.Event
import org.http4k.routing.bind as httpBind

/**
 * This is the main entry point for the MCP server. It handles the various MCP messages on both HTTP and SSE.
 */
fun McpHandler(mcpProtocol: SseMcpProtocol, json: AutoMarshallingJson<JsonNode> = McpJson) = poly(
    "/sse" bind sse {
        it.send(
            Event(
                "endpoint",
                Request(GET, "/message").with(sessionId of mcpProtocol.newSession(it)).uri.toString()
            )
        )
    },
    CatchLensFailure()
        .then(routes(
            "/message" httpBind POST to { req: Request ->
                mcpProtocol(sessionId(req), Body.jsonRpcRequest(json).toLens()(req), req)
            }
        ))
)

private val sessionId = Query.value(SessionId).required("sessionId")
