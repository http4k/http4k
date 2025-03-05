package org.http4k.mcp.server.protocol

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.mcp.protocol.SessionId

interface McpTransport<RSP : Any, Sink> : McpResponder<RSP> {
    fun newSession(connectRequest: Request, sink: Sink): SessionId
    fun start(executor: SimpleScheduler = SimpleSchedulerService(1))
}
