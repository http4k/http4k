package org.http4k.mcp.protocol

import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.Done.YES
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap

enum class Done {
    YES, NO
}

class ClientSession(client: VersionedMcpEntity, val capabilities: ClientCapabilities) {
    private val calls: MutableMap<RequestId, (JsonRpcResult<McpNodeType>) -> Done> = ConcurrentHashMap()

    fun addCall(requestId: RequestId, callback: (JsonRpcResult<McpNodeType>) -> Done) {
        calls[requestId] = callback
    }

    fun processResult(id: RequestId, result: JsonRpcResult<MoshiNode>) {
        val done = calls[id]?.invoke(result) ?: YES
        if (done == YES) calls.remove(id)
    }

    val entity = client.name
}
