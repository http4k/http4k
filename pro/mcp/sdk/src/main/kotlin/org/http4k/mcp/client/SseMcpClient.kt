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

class SseMcpClient(
    name: McpEntity,
    version: Version,
    capabilities: ClientCapabilities,
    sseRequest: Request,
    http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    protocolVersion: ProtocolVersion = LATEST_VERSION,
    defaultTimeout: Duration = Duration.ofSeconds(1)
) : AbstractMcpClient(VersionedMcpEntity(name, version), capabilities, protocolVersion, defaultTimeout) {

    private val http = SetHostFrom(sseRequest.uri).then(http)

    private val endpoint = AtomicReference<String>()

    private val sseClient = Http4kSseClient(sseRequest.with(MCP_PROTOCOL_VERSION of version), http)

    override fun received() = sseClient.received()

    override fun endpoint(it: Event) {
        endpoint.set(it.data)
    }

    override fun notify(rpc: McpRpc, mcp: ClientMessage.Notification): McpResult<Unit> {
        val response = http(mcp.toHttpRequest(rpc))
        return when {
            response.status.successful -> Success(Unit)
            else -> Failure(Http(response))
        }
    }

    override fun performRequest(
        rpc: McpRpc,
        request: ClientMessage,
        timeout: Duration,
        isComplete: (McpNodeType) -> Boolean
    ): McpResult<RequestId> {
        val requestId = RequestId.random()

        val latch = CountDownLatch(if (request is ClientMessage.Notification) 0 else 1)

        requests[requestId] = latch
        messageQueues[requestId] = ArrayBlockingQueue(1000)

        val response = http(request.toHttpRequest(rpc, requestId))
        return when {
            response.status.successful -> {
                resultFrom {
                    latch.await(timeout.toMillis(), MILLISECONDS)
                    Success(requestId)
                }.valueOrNull() ?: Timeout.failWith(requestId)
            }

            else -> Http(response).failWith(requestId)
        }
    }

    private fun ClientMessage.toHttpRequest(rpc: McpRpc, requestId: RequestId? = null) =
        Request(POST, Uri.of(endpoint.get()))
            .contentType(APPLICATION_JSON)
            .body(with(McpJson) {
                compact(
                    renderRequest(
                        rpc.Method.value,
                        asJsonObject(this@toHttpRequest),
                        requestId?.let { asJsonObject(it) } ?: nullNode())
                )
            })

    override fun close() {
        super.close()
        sseClient.close()
    }
}
