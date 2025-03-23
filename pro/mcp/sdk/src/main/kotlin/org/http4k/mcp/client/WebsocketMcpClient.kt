package org.http4k.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.mcp.client.McpError.Internal
import org.http4k.mcp.client.McpError.Timeout
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.MessageId
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.mcp.protocol.MCP_PROTOCOL_VERSION
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
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.random.Random

/**
 * WS connection MCP client.
 */
class WebsocketMcpClient(
    name: McpEntity,
    version: Version,
    private val wsRequest: Request,
    private val websocketFactory: WebsocketFactory,
    capabilities: ClientCapabilities = All,
    protocolVersion: ProtocolVersion = LATEST_VERSION,
    defaultTimeout: Duration = Duration.ofSeconds(1),
    random: Random = Random
) : AbstractMcpClient(VersionedMcpEntity(name, version), capabilities, protocolVersion, defaultTimeout, random) {
    private val wsClient by lazy {
        websocketFactory.blocking(
            wsRequest.uri,
            wsRequest.with(MCP_PROTOCOL_VERSION of protocolVersion).headers
        )
    }

    override fun received() = wsClient.received().map { SseMessage.parse(it.bodyString()) }

    override fun endpoint(it: Event) {}

    override fun notify(rpc: McpRpc, mcp: ClientMessage.Notification) = with(McpJson) {
        wsClient.send(WsMessage(compact(renderRequest(rpc.Method.value, asJsonObject(mcp), nullNode()))))
        Success(Unit)
    }

    override fun sendMessage(
        rpc: McpRpc,
        message: ClientMessage,
        timeout: Duration,
        messageId: MessageId,
        isComplete: (McpNodeType) -> Boolean
    ): Result<MessageId, McpError> {
        val latch = CountDownLatch(if (message is ClientMessage.Notification) 0 else 1)

        return resultFrom {
            requests[messageId] = latch
            messageQueues[messageId] = ArrayBlockingQueue(100)

            with(McpJson) {
                val payload = asJsonObject(message)

                wsClient
                    .send(
                        WsMessage(
                            compact(
                                when (message) {
                                    is ClientMessage.Response -> renderResult(payload, asJsonObject(messageId))
                                    else -> renderRequest(rpc.Method.value, payload, asJsonObject(messageId))
                                }
                            )
                        )
                    )
            }
            messageId
        }
            .flatMapFailure { Failure(Internal(it)) }
            .flatMap { reqId ->
                resultFrom {
                    if (!latch.await(timeout.toMillis(), MILLISECONDS)) error("Timeout waiting for init")
                }.flatMapFailure { Timeout.failWith(messageId) }
                    .map { reqId }
            }
    }

    override fun close() {
        super.close()
        wsClient.close()
    }
}
