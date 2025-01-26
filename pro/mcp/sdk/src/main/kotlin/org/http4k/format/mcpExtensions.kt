package org.http4k.format

import org.http4k.jsonrpc.jsonRpcVersion
import org.http4k.mcp.protocol.messages.McpNotification
import org.http4k.mcp.util.McpJson

fun McpJson.renderNotification(notification: McpNotification) = this {
    obj(listOf("jsonrpc" to string(jsonRpcVersion)) + fields(asJsonObject(notification)))
}
