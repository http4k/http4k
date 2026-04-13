/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.server.protocol.LogFunction
import org.http4k.ai.mcp.server.protocol.Logger
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap

fun logger(): Logger = ServerLogger()

private class ServerLogger : Logger {

    private val logLevels = ConcurrentHashMap<Session, LogLevel>()
    private val subscriptions = ConcurrentHashMap<Session, LogFunction>()

    override fun subscribe(session: Session, level: LogLevel, onLog: LogFunction) {
        logLevels[session] = level
        subscriptions[session] = onLog
    }

    override fun unsubscribe(session: Session) {
        logLevels.remove(session)
        subscriptions.remove(session)
    }

    override fun setLevel(session: Session, newLevel: LogLevel) {
        logLevels[session] = newLevel
    }

    override fun levelFor(session: Session) = logLevels[session] ?: LogLevel.info

    override fun log(session: Session, data: McpNodeType, level: LogLevel, logger: String?) {
        val minimum = logLevels[session] ?: LogLevel.error
        subscriptions[session]?.also { if (level >= minimum) it(data, level, logger) }
    }
}
