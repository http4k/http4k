/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.InitializeHandler
import org.http4k.ai.mcp.InitializeRequest
import org.http4k.ai.mcp.InitializeResponse.Error
import org.http4k.ai.mcp.InitializeResponse.Ok
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.server.protocol.Initializer
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest

class ServerInitializer(private val handler: InitializeHandler) : Initializer {
    override fun invoke(req: McpInitialize.Request, http: Request) =
        when (val response = handler(InitializeRequest(req.clientInfo, req.capabilities, req.protocolVersion))) {
            is Ok -> McpInitialize.Response(
                response.serverInfo,
                response.capabilities,
                response.protocolVersion,
                response.instructions
            )

            is Error -> throw McpException(InvalidRequest)
        }
}
