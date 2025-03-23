package org.http4k.mcp.client.internal

import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.model.MessageId
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import kotlin.reflect.KClass

internal class McpCallback<T : Any>(
    private val clazz: KClass<T>,
    private val callback: (T, MessageId?) -> Unit
) {
    operator fun invoke(req: JsonRpcRequest<McpNodeType>, messageId: MessageId?) {
        callback(
            McpJson.asA(McpJson.asFormatString(req.params ?: McpJson.nullNode()), clazz),
            messageId
        )
    }
}
