package org.http4k.mcp.client

import org.http4k.client.Http4kSseClient
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.renderRequest
import org.http4k.lens.contentType
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
import org.http4k.sse.SseMessage.Event
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class SseMcpClient(
    name: McpEntity,
    version: Version,
    capabilities: ClientCapabilities,
    sseRequest: Request,
    http: HttpHandler,
    protocolVersion: ProtocolVersion = LATEST_VERSION,
) : AbstractMcpClient(VersionedMcpEntity(name, version), capabilities, protocolVersion) {

    private val http = ClientFilters.SetHostFrom(sseRequest.uri).then(http)

    private val endpoint = AtomicReference<String>()

    private val sseClient = Http4kSseClient(sseRequest, http)

    override fun received() = sseClient.received()

    override fun endpoint(it: Event) {
        endpoint.set(it.data)
    }

    override fun notify(rpc: McpRpc, mcp: ClientMessage.Notification): Result<Unit> {
        val response = http(mcp.toHttpRequest(rpc))
        return when {
            response.status.successful -> runCatching { Unit }
            else -> runCatching { error("Failed HTTP ${response.status}") }
        }
    }

    override fun performRequest(rpc: McpRpc, request: ClientMessage, isComplete: (McpNodeType) -> Boolean)
        : Result<RequestId> {
        val requestId = RequestId.random()

        val latch = CountDownLatch(if (request is ClientMessage.Notification) 0 else 1)

        requests[requestId] = latch
        messageQueues[requestId] = ArrayBlockingQueue(100)

        val response = http(request.toHttpRequest(rpc, requestId))
        return runCatching {
            when {
                response.status.successful -> {
                    latch.await()
                    requestId
                }

                else -> {
                    tidyUp(requestId)
                    latch.await()
                    error("Failed HTTP ${response.status}")
                }
            }
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
