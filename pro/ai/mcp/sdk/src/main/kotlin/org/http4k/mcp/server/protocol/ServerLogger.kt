package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.LogLevel
import java.util.concurrent.ConcurrentHashMap

class ServerLogger : Logger {

    private val subscriptions = ConcurrentHashMap<Session, Pair<LogLevel, LogFunction>>()

    override fun subscribe(session: Session, level: LogLevel, onLog: LogFunction) {
        subscriptions[session] = level to onLog
    }

    override fun unsubscribe(session: Session) {
        subscriptions.remove(session)
    }

    override fun setLevel(session: Session, newLevel: LogLevel) {
        subscriptions[session]?.also { (_, logFunction) ->
            subscriptions[session] = newLevel to logFunction
        }

    }

    override fun log(session: Session, level: LogLevel, logger: String, data: Map<String, Any>) {
        subscriptions[session]?.also { (actualLevel, logFunction) ->
            if (level >= actualLevel) logFunction(level, logger, data)
        }
    }
}
