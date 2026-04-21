/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.jsonrpc

import org.http4k.ai.mcp.server.asHttp
import org.http4k.ai.mcp.server.protocol.InvalidSessionState
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.ValidSessionState
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.routing.bind

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing and immediate response
 * via JSON RPC result messages.
 */
fun JsonRpcMcpConnection(protocol: McpProtocol<Unit>) = "/jsonrpc" bind { req: Request ->
    when (val sessionState = protocol.retrieveSession(req)) {
        is ValidSessionState -> {
            protocol.receive(Unit, sessionState, req).asHttp(OK)
                    .with(Header.MCP_SESSION_ID of sessionState.session.id)
            }

        InvalidSessionState -> Response(NOT_FOUND)
    }
}
