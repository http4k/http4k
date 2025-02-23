package org.http4k.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.Request
import org.http4k.format.renderRequest
import org.http4k.mcp.client.McpError.Internal
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.Version
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

/**
 * Single connection MCP client.
 */
class WsMcpClient(
    name: McpEntity,
    version: Version,
    capabilities: ClientCapabilities,
    private val wsRequest: Request,
    private val websocketFactory: WebsocketFactory,
    protocolVersion: ProtocolVersion = LATEST_VERSION,
) : AbstractMcpClient(VersionedMcpEntity(name, version), capabilities, protocolVersion) {
    private val wsClient by lazy { websocketFactory.blocking(wsRequest.uri, wsRequest.headers) }

    override fun received() = wsClient.received().map { SseMessage.parse(it.bodyString()) }

    override fun endpoint(it: Event) {}

    override fun notify(rpc: McpRpc, mcp: ClientMessage.Notification) = with(McpJson) {
        wsClient.send(WsMessage(compact(renderRequest(rpc.Method.value, asJsonObject(mcp), nullNode()))))
        Success(Unit)
    }

    override fun performRequest(rpc: McpRpc, request: ClientMessage, isComplete: (McpNodeType) -> Boolean) =
        resultFrom {
            val latch = CountDownLatch(if (request is ClientMessage.Notification) 0 else 1)

            val requestId = RequestId.random()

            requests[requestId] = latch
            messageQueues[requestId] = ArrayBlockingQueue(100)

            with(McpJson) {
                wsClient
                    .send(
                        WsMessage(
                            compact(renderRequest(rpc.Method.value, asJsonObject(request), asJsonObject(requestId)))
                        )
                    )
            }
            latch.await()

            requestId
        }
            .flatMapFailure { Failure(Internal(it)) }

    override fun close() {
        super.close()
        wsClient.close()
    }
}
