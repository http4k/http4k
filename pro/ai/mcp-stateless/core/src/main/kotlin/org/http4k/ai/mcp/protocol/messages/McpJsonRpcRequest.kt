/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic

@JsonSerializable
@Polymorphic("method")
sealed class McpJsonRpcRequest : McpJsonRpcMessage {
    abstract override val id: Any?
    abstract val method: McpRpcMethod
    abstract val params: HasMeta?
}

// The Mcp-Name mirror header only applies to targeted requests; null means "no Mcp-Name expected".
// Single source of truth for both the client (stamps it) and the server (validates it).
fun McpJsonRpcRequest.mirroredName(): String? = when (this) {
    is McpTool.Call.Request -> params.name.value
    is McpPrompt.Get.Request -> params.name.value
    is McpResource.Read.Request -> params.uri.toString()
    else -> null
}
