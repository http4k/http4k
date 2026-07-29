/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.core.Method.POST
import org.http4k.core.Request

fun McpJsonRpcRequest.asMcpRequest() = McpRequest(Session(SessionId.of("test")), this, Request(POST, "/"))

fun McpJsonRpcMessage.asMcpResponse() = McpResponse.Ok(this)
