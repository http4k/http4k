package org.http4k.mcp

import dev.forkhandles.values.random
import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.Tool
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import kotlin.random.Random

class Sessions<NODE : Any>(
    private val serDe: Serde<NODE>,
    private val tools: Tools,
    private val resources: Resources,
    private val prompts: Prompts,
    private val random: Random
) {
    private val sessions = mutableMapOf<SessionId, Session<NODE>>()

    fun add(sse: Sse) {
        val sessionId = SessionId.random(random)

        val session = Session(sessionId, serDe, sse)
        sessions[sessionId] = session
        prompts.onChange(sessionId) {
            session.send(
                Prompt.List,
                Prompt.List.Notification
            )
        }
        resources.onChange(sessionId) { session.send(Resource.List, Resource.List.Notification) }
        tools.onChange(sessionId) { session.send(Tool.List, Tool.List.Notification) }

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
