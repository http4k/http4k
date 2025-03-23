package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.LogLevel

/**
 * Logs messages for a session back to the client.
 */
interface Logger {
    fun subscribe(session: Session, level: LogLevel, onLog: LogFunction)
    fun unsubscribe(session: Session)
    fun setLevel(session: Session, newLevel: LogLevel)
    fun log(session: Session, level: LogLevel, logger: String, data: Map<String, Any>)
}

fun interface LogFunction : (LogLevel, String, Map<String, Any>) -> Unit
