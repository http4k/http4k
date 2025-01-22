package org.http4k.mcp.server

import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Tools
import org.http4k.mcp.model.LogLevel.error
import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.mcp.protocol.McpLogging
import org.http4k.mcp.protocol.McpPrompt
import org.http4k.mcp.protocol.McpResource
import org.http4k.mcp.protocol.McpTool
import org.http4k.sse.Sse
import java.util.concurrent.ConcurrentHashMap

class ClientSessions<NODE : Any>(
    private val tools: Tools,
    private val resources: Resources,
    private val prompts: Prompts,
    private val logger: Logger,
    private val handler: McpMessageHandler<NODE>
) {
    private val sessions = ConcurrentHashMap<SessionId, Sse>()

    fun add(sse: Sse, sessionId: SessionId) {
        sessions[sessionId] = sse
        logger.subscribe(sessionId, error) { level, logger, data ->
            sse.send(handler(McpLogging.LoggingMessage(level, logger, data)))
        }
        prompts.onChange(sessionId) { sse.send(handler(McpPrompt.List.Changed)) }
        resources.onChange(sessionId) { sse.send(handler(McpResource.List.Changed)) }
        tools.onChange(sessionId) { sse.send(handler(McpTool.List.Changed)) }

        sse.onClose {
            prompts.remove(sessionId)
            resources.remove(sessionId)
            tools.remove(sessionId)
            sessions.remove(sessionId)

            logger.unsubscribe(sessionId)
        }
    }

    operator fun get(sessionId: SessionId) = sessions[sessionId]
}
