package org.http4k.ai.mcp.client.internal

import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import kotlin.reflect.KClass

internal class McpCallback<T : Any>(
    private val clazz: KClass<T>,
    private val callback: (T, McpMessageId?) -> Unit
) {
    operator fun invoke(req: JsonRpcRequest<McpNodeType>, messageId: McpMessageId?): Boolean =
        try {
            callback(
                McpJson.asA(McpJson.asFormatString(req.params ?: McpJson.nullNode()), clazz),
                messageId
            )
            true
        } catch (e: Exception) {
            false
        }
}
