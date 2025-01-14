package org.http4k.mcp

import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.Tool
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event

class Sessions<NODE : Any>(
    private val serDe: Serde<NODE>,
    private val tools: Tools,
    private val resources: Resources,
    private val prompts: Prompts,
) {
    private val sessions = mutableMapOf<SessionId, Sse>()

    fun add(sessionId: SessionId, sse: Sse) {
        prompts.onChange(sessionId) { sse.send(serDe, Prompt.List, Prompt.List.Notification) }
        resources.onChange(sessionId) { sse.send(serDe, Resource.List, Resource.List.Notification) }
        tools.onChange(sessionId) { sse.send(serDe, Tool.List, Tool.List.Notification) }

        sessions[sessionId] = sse

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
