package org.http4k.mcp.client.internal

import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import kotlin.reflect.KClass

internal class NotificationCallback<T : Any>(
    private val hasMethod: McpRpc,
    private val clazz: KClass<T>,
    private val callback: (T) -> Unit
) {
    fun process(req: JsonRpcRequest<McpNodeType>) {
        if (req.method == hasMethod.Method.value) {
            callback(McpJson.asA(McpJson.asFormatString(req.params ?: McpJson.nullNode()), clazz))
        }
    }
}
