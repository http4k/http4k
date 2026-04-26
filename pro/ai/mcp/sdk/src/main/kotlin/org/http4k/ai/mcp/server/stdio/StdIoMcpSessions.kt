/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.stdio

import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.server.protocol.ClientRequestContext
import org.http4k.ai.mcp.server.protocol.ExistingSession
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.Sessions
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.Request
import java.io.Writer
import java.util.UUID

class StdIoMcpSessions(private val writer: Writer) : Sessions<Unit> {

    override fun send(context: ClientRequestContext, message: McpJsonRpcMessage) = with(writer) {
        write(McpJson.asFormatString(message) + "\n")
        flush()
    }

    override fun onClose(context: ClientRequestContext, fn: () -> Unit) = fn()

    override fun retrieveSession(connectRequest: Request) =
        ExistingSession(Session(SessionId.of(UUID.randomUUID().toString())))

    override fun transportFor(context: ClientRequestContext) {
        error("not implemented")
    }

    override fun end(context: ClientRequestContext) {}

    override fun assign(context: ClientRequestContext, transport: Unit, connectRequest: Request) {}
}
