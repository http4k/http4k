/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
@JsonDefaultValue
data class UnknownMcpJsonRpcRequest(
    override val id: Any? = null,
    override val method: McpRpcMethod = McpRpcMethod.of("unknown"),
    override val params: Params? = null
) : McpJsonRpcRequest() {

    @JsonSerializable
    data class Params(override val _meta: Meta = Meta.default) : HasMeta
}
