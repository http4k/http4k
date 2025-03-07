package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.capability.LogFunction
import org.http4k.mcp.server.capability.Logger
import java.util.concurrent.ConcurrentHashMap

class ServerLogger : Logger {

    private val subscriptions = ConcurrentHashMap<SessionId, Pair<LogLevel, LogFunction>>()

    override fun subscribe(sessionId: SessionId, level: LogLevel, onLog: LogFunction) {
        subscriptions[sessionId] = level to onLog
    }

    override fun unsubscribe(sessionId: SessionId) {
        subscriptions.remove(sessionId)
    }

    override fun setLevel(sessionId: SessionId, newLevel: LogLevel) {
        subscriptions[sessionId]?.also { (_, logFunction) ->
            subscriptions[sessionId] = newLevel to logFunction
        }

    }

    override fun log(sessionId: SessionId, level: LogLevel, logger: String, data: Map<String, Any>) {
        subscriptions[sessionId]?.also { (actualLevel, logFunction) ->
            if (level >= actualLevel) logFunction(level, logger, data)
        }
    }
}
