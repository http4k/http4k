package org.http4k.ai.mcp.client.websocket

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.resultFrom
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.McpError.Internal
import org.http4k.ai.mcp.McpError.Timeout
import org.http4k.ai.mcp.client.AbstractMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.WebsocketFactory
import org.http4k.websocket.WsMessage
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * WS connection MCP client.
 *
 * Note that the internal representation uses an unbounded blocking queue, so clients are required to consume messages
 * using received().
 */
class WebsocketMcpClient(
    name: McpEntity,
    version: Version,
    private val wsRequest: Request,
    private val websocketFactory: WebsocketFactory,
    capabilities: ClientCapabilities = All,
    protocolVersion: ProtocolVersion = LATEST_VERSION,
    title: String? = null,
    defaultTimeout: Duration = Duration.ofSeconds(1),
    random: Random = Random
) : AbstractMcpClient(VersionedMcpEntity(name, version, title), capabilities, protocolVersion, defaultTimeout, random) {
    private val wsClient by lazy {
        websocketFactory.blocking(
            wsRequest.uri,
            wsRequest.with(Header.MCP_PROTOCOL_VERSION of protocolVersion).headers
        )
    }

    override fun received() = wsClient.received().map { SseMessage.parse(it.bodyString()) }

    override fun endpoint(it: Event) {
        endpoint.set(it.data)
    }

    private val endpoint = AtomicReference<String>()

    override val sessionId
        get() =
            SessionId.parse(Request(GET, endpoint.get().toString()).query("sessionId") ?: "-")

    override fun notify(rpc: McpRpc, mcp: ClientMessage.Notification) = with(McpJson) {
        wsClient.send(WsMessage(compact(renderRequest(rpc.Method.value, asJsonObject(mcp), nullNode()))))
        Success(Unit)
    }

    override fun sendMessage(
        rpc: McpRpc,
        message: ClientMessage,
        timeout: Duration,
        messageId: McpMessageId,
        isComplete: (McpNodeType) -> Boolean
    ): Result<McpMessageId, McpError> {
        val latch =
            CountDownLatch(if (message is ClientMessage.Notification || message is ClientMessage.Response) 0 else 1)

        return resultFrom {
            requests[messageId] = latch
            messageQueues[messageId] = LinkedBlockingQueue()

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
