/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic

@JsonSerializable
@Polymorphic("method")
sealed class McpJsonRpcRequest : McpJsonRpcMessage {
    abstract override val id: Any?
    abstract val method: McpRpcMethod
    abstract val params: HasMeta?
}

fun McpJsonRpcRequest.mirroredName(): String? = (params as? HasMirroredName)?.mirroredName()
