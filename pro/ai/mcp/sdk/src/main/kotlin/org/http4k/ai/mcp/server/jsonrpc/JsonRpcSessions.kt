/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.jsonrpc

import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.server.protocol.ClientRequestContext
import org.http4k.ai.mcp.server.protocol.Sessions
import org.http4k.ai.mcp.server.sessions.SessionProvider
import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import kotlin.random.Random

class JsonRpcSessions(
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random)
) :
    Sessions<Unit> {

    override fun send(context: ClientRequestContext, message: McpJsonRpcMessage) {
        // Server-to-client notifications are not supported in non-streaming JSON-RPC mode
    }

    override fun onClose(context: ClientRequestContext, fn: () -> Unit) {
    }

    override fun retrieveSession(connectRequest: Request) =
        sessionProvider.validate(connectRequest, Header.MCP_SESSION_ID(connectRequest))

    override fun transportFor(context: ClientRequestContext) {
        error("Unsupported")
    }

    override fun assign(context: ClientRequestContext, transport: Unit, connectRequest: Request) {
    }

    override fun end(context: ClientRequestContext) {

    }
}
