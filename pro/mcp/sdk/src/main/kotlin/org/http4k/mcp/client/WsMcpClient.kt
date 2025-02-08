package org.http4k.mcp.client

import org.http4k.core.Request
import org.http4k.format.renderRequest
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.WebsocketFactory
import org.http4k.websocket.WsMessage
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import kotlin.Result.Companion.success

/**
 * Single connection MCP client.
 */
class WsMcpClient(
    private val wsRequest: Request,
    private val websocketFactory: WebsocketFactory,
    clientInfo: VersionedMcpEntity,
    capabilities: ClientCapabilities,
    protocolVersion: ProtocolVersion = LATEST_VERSION,
) : AbstractMcpClient(clientInfo, capabilities, protocolVersion) {
    private val wsClient by lazy { websocketFactory.blocking(wsRequest.uri, wsRequest.headers) }

    override fun received() = wsClient.received().map { SseMessage.parse(it.bodyString()) }

    override fun endpoint(it: Event) {}

    override fun notify(rpc: McpRpc, mcp: ClientMessage.Notification) = with(McpJson) {
        wsClient.send(WsMessage(compact(renderRequest(rpc.Method.value, asJsonObject(mcp), nullNode()))))
        success(Unit)
    }

    override fun performRequest(rpc: McpRpc, request: ClientMessage, isComplete: (McpNodeType) -> Boolean) =
        runCatching {
            val latch = CountDownLatch(if (request is ClientMessage.Notification) 0 else 1)

            val requestId = RequestId.random()

            requests[requestId] = latch
            messageQueues[requestId] = ArrayBlockingQueue(100)

            with(McpJson) {
                wsClient.send(
                    WsMessage(compact(renderRequest(rpc.Method.value, asJsonObject(request), asJsonObject(requestId))))
                )
            }
            latch.await()

            requestId
        }

    override fun close() {
        super.close()
        wsClient.close()
    }
}
