package org.http4k.mcp.server.session

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.McpResponder

interface McpConnection<RSP : Any, Sink> : McpResponder<RSP> {
    fun new(connectRequest: Request, sink: Sink): SessionId
    fun onClose(sessionId: SessionId, fn: () -> Unit)
    fun start(executor: SimpleScheduler = SimpleSchedulerService(1))
}
