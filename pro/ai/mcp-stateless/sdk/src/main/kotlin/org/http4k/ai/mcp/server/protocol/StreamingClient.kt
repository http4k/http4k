/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.Request
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

internal class StreamingClient(private val sse: Sse, private val logLevel: LogLevel?) : Client {

    override fun progress(progressToken: ProgressToken, progress: Int, total: Double?, description: String?) {
        sse.send(subscriptionEvent(McpProgress.Notification(McpProgress.Notification.Params(progressToken, progress, total, description))))
    }

    override fun log(data: Any, level: LogLevel, logger: String?) {
        if (logLevel != null && level >= logLevel) {
            sse.send(
                subscriptionEvent(
                    McpLogging.LoggingMessage.Notification(
                        McpLogging.LoggingMessage.Notification.Params(McpJson.asJsonObject(data), level, logger)
                    )
                )
            )
        }
    }
}

internal class FakeSse(override val connectRequest: Request) : Sse {
    override fun send(message: SseMessage) = this
    override fun close() {}
    override fun onClose(fn: () -> Unit) = this
}
