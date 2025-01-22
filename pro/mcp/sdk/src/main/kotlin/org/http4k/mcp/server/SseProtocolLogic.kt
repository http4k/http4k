package org.http4k.mcp.server

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools
import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.mcp.processing.Serde
import org.http4k.mcp.protocol.MessageId
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage
import kotlin.random.Random

class SseProtocolLogic(
    metaData: ServerMetaData,
    tools: Tools,
    completions: Completions,
    resources: Resources,
    roots: Roots,
    sampling: Sampling,
    handler: McpMessageHandler<JsonNode>,
    prompts: Prompts,
    serDe: Serde<JsonNode>,
    logger: Logger,
    private val sessions: ClientSessions<JsonNode>,
    random: Random,
    calls: MutableMap<MessageId, (JsonRpcResult<JsonNode>) -> Unit>,
    json: McpJson
) : McpProtocolLogic(
    metaData,
    tools,
    completions,
    resources,
    roots,
    sampling,
    handler,
    prompts,
    serDe,
    logger,
    random,
    calls,
    json,
) {
    override fun unit(unit: Unit) = Response(ACCEPTED)

    override fun send(message: SseMessage, sessionId: SessionId) = when (val session = sessions[sessionId]) {
        null -> Response(GONE)
        else -> Response(ACCEPTED).also { session.send(message) }
    }

    override fun error() = Response(GONE)
}
