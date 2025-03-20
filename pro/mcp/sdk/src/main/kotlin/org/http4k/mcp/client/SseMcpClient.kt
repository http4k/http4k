package org.http4k.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.resultFrom
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.Http4kSseClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.lens.contentType
import org.http4k.mcp.client.McpError.Http
import org.http4k.mcp.client.McpError.Timeout
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.MCP_PROTOCOL_VERSION
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage.Event
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
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
    capabilities: ClientCapabilities = ClientCapabilities(),
    protocolVersion: ProtocolVersion = LATEST_VERSION,
    defaultTimeout: Duration = Duration.ofSeconds(1),
    random: Random = Random
) : AbstractMcpClient(VersionedMcpEntity(name, version), capabilities, protocolVersion, defaultTimeout, random) {

    private val http = SetHostFrom(sseRequest.uri).then(http)

    private val endpoint = AtomicReference<String>()

    private val sseClient = Http4kSseClient(sseRequest.with(MCP_PROTOCOL_VERSION of protocolVersion), http)

    override fun received() = sseClient.received()

    override fun endpoint(it: Event) {
        endpoint.set(it.data)
    }

    override fun notify(rpc: McpRpc, mcp: ClientMessage.Notification): McpResult<Unit> {
        val response = http(mcp.toHttpRequest(Uri.of(endpoint.get()), rpc))
        return when {
            response.status.successful -> Success(Unit)
            else -> Failure(Http(response))
        }
    }

    override fun sendMessage(
        rpc: McpRpc,
        message: ClientMessage,
        timeout: Duration,
        requestId: RequestId,
        isComplete: (McpNodeType) -> Boolean
    ): McpResult<RequestId> {
        val latch = CountDownLatch(if (message is ClientMessage.Notification) 0 else 1)

        requests[requestId] = latch

        if (messageQueues[requestId] == null) messageQueues[requestId] = ArrayBlockingQueue(1000)

        val response = http(message.toHttpRequest(Uri.of(endpoint.get()), rpc, requestId))
        return when {
            response.status.successful -> resultFrom {
                latch.await(timeout.toMillis(), MILLISECONDS)
                Success(requestId)
            }.valueOrNull() ?: Timeout.failWith(requestId)

            else -> Http(response).failWith(requestId)
        }
    }

    override fun close() {
        super.close()
        sseClient.close()
    }
}

internal fun ClientMessage.toHttpRequest(endpoint: Uri, rpc: McpRpc, requestId: RequestId? = null) =
    Request(POST, endpoint)
        .contentType(APPLICATION_JSON)
        .body(with(McpJson) {
            val params = asJsonObject(this@toHttpRequest)
            val id = requestId?.let { asJsonObject(it) } ?: nullNode()
            compact(
                when (this@toHttpRequest) {
                    is ClientMessage.Response -> renderResult(params, id)
                    else -> renderRequest(rpc.Method.value, params, id)
                }
            )
        })
