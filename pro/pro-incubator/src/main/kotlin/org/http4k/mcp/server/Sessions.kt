package org.http4k.mcp.server

import dev.forkhandles.values.random
import org.http4k.connect.mcp.protocol.McpPrompt
import org.http4k.connect.mcp.protocol.McpResource
import org.http4k.connect.mcp.protocol.McpTool
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.mcp.Serde
import org.http4k.mcp.SessionId
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class Sessions<NODE : Any>(
    private val serDe: Serde<NODE>,
    private val tools: McpTools,
    private val resources: McpResources,
    private val prompts: McpPrompts,
    private val random: Random
) {
    private val sessions = ConcurrentHashMap<SessionId, Session<NODE>>()

    fun add(sse: Sse) {
        val sessionId = SessionId.random(random)

        val session = Session(sessionId, serDe, sse)
        sessions[sessionId] = session
        prompts.onChange(sessionId) { session.send(McpPrompt.List.Changed) }
        resources.onChange(sessionId) { session.send(McpResource.List.Changed) }
        tools.onChange(sessionId) { session.send(McpTool.List.Changed) }

        sse.onClose {
            prompts.remove(sessionId)
            resources.remove(sessionId)
            tools.remove(sessionId)

            sessions.remove(sessionId)
        }
        sse.send(Event("endpoint", Uri.of("/message").query("sessionId", sessionId.value.toString()).toString()))
    }

    operator fun get(sessionId: SessionId) = sessions[sessionId]
}
