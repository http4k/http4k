/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpJsonRpcEmptyResponse(override val id: Any?, val result: Map<String, Any> = emptyMap(), val jsonrpc: String = "2.0") : McpJsonRpcResponse()
