package org.http4k.mcp.server

import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.format.jsonRpcRequest
import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools
import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.mcp.util.McpJson
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import kotlin.random.Random
import org.http4k.routing.bind as httpBind

/**
 * This is the main entry point for the MCP server. It handles the various MCP messages on both HTTP and SSE.
 */
fun McpHandler(
    metaData: ServerMetaData,
    prompts: Prompts = Prompts(emptyList()),
    tools: Tools = Tools(emptyList()),
    resources: Resources = Resources(emptyList()),
    completions: Completions = Completions(emptyList()),
    sampling: Sampling = Sampling(emptyList()),
    roots: Roots = Roots(),
    logger: Logger = Logger(),
    random: Random = Random
): PolyHandler {
    val json = McpJson

    val handler = McpMessageHandler(json)
    val sessions = ClientSessions(tools, resources, prompts, logger, random, handler)

    val protocolLogic = SseProtocolLogic(
        sessions,
        handler, metaData, tools, completions, resources, roots,
        sampling, prompts, logger, random, json
    )

    return poly(
        "/sse" bind sse { sessions.add(it) },
        routes(
            "/message" httpBind POST to { req: Request ->
                protocolLogic(SessionId.parse(req.query("sessionId")!!), Body.jsonRpcRequest(json).toLens()(req), req)
            }
        )
    )
}
