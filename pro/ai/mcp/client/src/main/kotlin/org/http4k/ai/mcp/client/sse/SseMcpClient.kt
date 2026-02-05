package org.http4k.ai.mcp.client.sse

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.resultFrom
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.McpError.Http
import org.http4k.ai.mcp.McpError.Timeout
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.client.AbstractMcpClient
import org.http4k.ai.mcp.client.toHttpRequest
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
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.client.Http4kSseClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.sse.SseMessage.Event
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * SSE connection MCP client.
 */
class SseMcpClient(
    name: McpEntity,
    version: Version,
    sseRequest: Request,
    http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    capabilities: ClientCapabilities = All,
    protocolVersion: ProtocolVersion = LATEST_VERSION,
    defaultTimeout: Duration = Duration.ofSeconds(1),
    random: Random = Random
) : AbstractMcpClient(VersionedMcpEntity(name, version), capabilities, protocolVersion, defaultTimeout, random) {

    private val http = SetHostFrom(sseRequest.uri).then(http)

    private val endpoint = AtomicReference<String>()

    private val sseClient = Http4kSseClient(sseRequest.with(Header.MCP_PROTOCOL_VERSION of protocolVersion), http)

    override fun received() = sseClient.received()

    override fun endpoint(it: Event) {
        endpoint.set(it.data)
    }

    override fun notify(rpc: McpRpc, mcp: ClientMessage.Notification): McpResult<Unit> {
        val response = http(mcp.toHttpRequest(protocolVersion, Uri.of(endpoint.get()), rpc))
        return when {
            response.status.successful -> Success(Unit)
            else -> Failure(Http(response))
        }
    }

    override fun sendMessage(
        rpc: McpRpc,
        message: ClientMessage,
        timeout: Duration,
        messageId: McpMessageId,
        isComplete: (McpNodeType) -> Boolean
    ): McpResult<McpMessageId> {
        val latch =
            CountDownLatch(if (message is ClientMessage.Notification || message is ClientMessage.Response) 0 else 1)

        requests[messageId] = latch

        if (messageQueues[messageId] == null) messageQueues[messageId] = LinkedBlockingQueue()

        val response = http(message.toHttpRequest(protocolVersion, Uri.of(endpoint.get()), rpc, messageId))
        return when {
            response.status.successful -> resultFrom {
                if (!latch.await(timeout.toMillis(), MILLISECONDS)) error("Timeout waiting for init")
                Success(messageId)
            }.valueOrNull() ?: Timeout.failWith(messageId)

            else -> Http(response).failWith(messageId)
        }
    }

    override fun close() {
        super.close()
        sseClient.close()
    }

    override val sessionId
        get() =
            SessionId.parse(Request(GET, endpoint.get().toString()).query("sessionId") ?: "-")
}
