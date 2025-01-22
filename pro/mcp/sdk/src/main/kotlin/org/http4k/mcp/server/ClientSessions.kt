package org.http4k.mcp.server

import dev.forkhandles.values.random
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Tools
import org.http4k.mcp.model.LogLevel.error
import org.http4k.mcp.protocol.McpLogging
import org.http4k.mcp.protocol.McpPrompt
import org.http4k.mcp.protocol.McpResource
import org.http4k.mcp.protocol.McpTool
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class ClientSessions<NODE : Any>(
    private val serDe: Serde<NODE>,
    private val tools: Tools,
    private val resources: Resources,
    private val prompts: Prompts,
    private val logger: Logger,
    private val random: Random
) {
    private val sessions = ConcurrentHashMap<SessionId, ClientSession<NODE>>()

    fun add(sse: Sse) {
        val sessionId = SessionId.random(random)

        val session = ClientSession(serDe, sse)
        sessions[sessionId] = session
        logger.subscribe(sessionId, error) { level, logger, data ->
            session.send(McpLogging.LoggingMessage(level, logger, data))
        }
        prompts.onChange(sessionId) { session.send(McpPrompt.List.Changed) }
        resources.onChange(sessionId) { session.send(McpResource.List.Changed) }
        tools.onChange(sessionId) { session.send(McpTool.List.Changed) }

        sse.onClose {
            prompts.remove(sessionId)
            resources.remove(sessionId)
            tools.remove(sessionId)
            sessions.remove(sessionId)

            logger.unsubscribe(sessionId)
        }
        sse.send(Event("endpoint", Uri.of("/message").query("sessionId", sessionId.value.toString()).toString()))
    }

    operator fun get(sessionId: SessionId) = sessions[sessionId]
}
