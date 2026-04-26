/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.jsonrpc.ErrorMessage
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpJsonRpcErrorResponse(override val id: Any?, val error: McpNodeType, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
    constructor(id: Any?, error: ErrorMessage) : this(id, error(org.http4k.ai.mcp.util.McpJson))
}
