package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.util.McpNodeType

/**
 * Logs messages for a session back to the client.
 */
interface Logger {
    fun subscribe(session: Session, level: LogLevel, onLog: LogFunction)
    fun unsubscribe(session: Session)
    fun setLevel(session: Session, newLevel: LogLevel)
    fun levelFor(session: Session): LogLevel
    fun log(session: Session, data: McpNodeType, level: LogLevel, logger: String? = null)
}

fun interface LogFunction : (McpNodeType, LogLevel, String?) -> Unit
