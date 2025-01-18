package org.http4k.format

import org.http4k.jsonrpc.jsonRpcVersion
import org.http4k.mcp.protocol.McpNotification

fun <NODE : Any> AutoMarshallingJson<NODE>.renderNotification(notification: McpNotification): NODE = this {
    obj(listOf("jsonrpc" to string(jsonRpcVersion)) + fields(asJsonObject(notification)))
}
