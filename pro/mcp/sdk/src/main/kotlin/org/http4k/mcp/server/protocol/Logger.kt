package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.protocol.SessionId

/**
 * Logs messages for a session back to the client.
 */
interface Logger {
    fun subscribe(sessionId: SessionId, level: LogLevel, onLog: LogFunction)
    fun unsubscribe(sessionId: SessionId)
    fun setLevel(sessionId: SessionId, newLevel: LogLevel)
    fun log(sessionId: SessionId, level: LogLevel, logger: String, data: Map<String, Any>)
}

fun interface LogFunction : (LogLevel, String, Map<String, Any>) -> Unit
