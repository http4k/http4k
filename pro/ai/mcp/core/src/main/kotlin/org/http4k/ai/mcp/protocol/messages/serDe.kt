/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.format.renderError
import org.http4k.format.renderRequest
import org.http4k.jsonrpc.ErrorMessage

fun McpWireRequest.toJsonRpc(method: McpRpc, id: McpNodeType) =
    McpJson.renderRequest(method.Method.value, McpJson.asJsonObject(this), id)

fun McpWireNotification.toJsonRpc(method: McpRpc) =
    McpJson.renderRequest(method.Method.value, McpJson.asJsonObject(this), McpJson.nullNode())

fun ErrorMessage.toJsonRpc(id: Any?) = McpJson.renderError(this, id.toMcpNode())

private fun Any?.toMcpNode(): McpNodeType = when(this) {
    is McpNodeType -> this
    null -> McpJson.nullNode()
    else -> McpJson.asJsonObject(this)
}
