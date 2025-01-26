package org.http4k.mcp.capability

import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.protocol.SessionId
import java.util.concurrent.ConcurrentHashMap

class Logger {

    private val subscriptions = ConcurrentHashMap<SessionId, Pair<LogLevel, LogFunction>>()

    fun subscribe(sessionId: SessionId, level: LogLevel, onLog: LogFunction) {
        subscriptions[sessionId] = level to onLog
    }

    fun unsubscribe(sessionId: SessionId) {
        subscriptions.remove(sessionId)
    }

    fun setLevel(sessionId: SessionId, newLevel: LogLevel) {
        subscriptions[sessionId]?.also { (_, logFunction) ->
            subscriptions[sessionId] = newLevel to logFunction
        }

    }

    fun log(sessionId: SessionId, level: LogLevel, logger: String, data: Map<String, Any>) {
        subscriptions[sessionId]?.also { (actualLevel, logFunction) ->
            if (level >= actualLevel) logFunction(level, logger, data)
        }
    }
}

fun interface LogFunction : (LogLevel, String, Map<String, Any>) -> Unit
