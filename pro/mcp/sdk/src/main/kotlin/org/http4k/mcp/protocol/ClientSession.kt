package org.http4k.mcp.protocol

import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap

class ClientSession(client: VersionedMcpEntity, val capabilities: ClientCapabilities) {
    private val calls: MutableMap<RequestId, (JsonRpcResult<McpNodeType>) -> Unit> = ConcurrentHashMap()

    fun addCall(requestId: RequestId, callback: (JsonRpcResult<McpNodeType>) -> Unit) {
        calls[requestId] = callback
    }

    fun processResult(id: RequestId, result: JsonRpcResult<MoshiNode>) = calls.remove(id)?.invoke(result)

    val entity = client.name
}
