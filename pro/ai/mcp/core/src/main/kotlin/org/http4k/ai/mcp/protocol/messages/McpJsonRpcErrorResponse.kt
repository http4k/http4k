/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpJsonRpcErrorResponse(override val id: McpNodeType?, val error: McpError) : McpJsonRpcResponse
