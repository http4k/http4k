package org.http4k.ai.mcp.testing

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.accept
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpJson
import org.http4k.sse.SseMessage
import org.http4k.testing.testSseClient
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


class TestMcpSender(private val mcpHandler: PolyHandler, private val connectRequest: Request) {

    private val outbound = mutableMapOf<McpRpcMethod, MutableList<(SseMessage.Event) -> Unit>>()

    fun on(mcpRpc: McpRpc, fn: (SseMessage.Event) -> Unit) {
        outbound.getOrPut(mcpRpc.Method) { mutableListOf() }.add(fn)
    }

    private fun filterOut(events: Sequence<SseMessage.Event>, mpcRpc: McpRpc) = events
        .filter {
            when {
                it.isFor(mpcRpc) || it.isResult() || it.isError() -> true
                else -> {
                    outbound[it.mcpMethod()]?.forEach { sub -> sub(it) }
                    false
                }
            }
        }

    private fun SseMessage.Event.isResult() = McpJson.fields(McpJson.parse(data)).toMap().containsKey("result")
    private fun SseMessage.Event.isError() = McpJson.fields(McpJson.parse(data)).toMap().containsKey("error")

    private fun SseMessage.Event.isFor(rpc: McpRpc) = mcpMethod() == rpc.Method

    private fun SseMessage.Event.mcpMethod() =
        McpJson.fields(McpJson.parse(data)).toMap()["method"]?.let { McpRpcMethod.of(McpJson.text(it)) }

    private var id = AtomicInteger(0)

    var sessionId = AtomicReference<SessionId>()

    fun stream() = mcpHandler.callWith(connectRequest.accept(TEXT_EVENT_STREAM).method(GET))

    operator fun invoke(mcpRpc: McpRpc, input: ClientMessage.Request) =
        filterOut(
            mcpHandler.callWith(connectRequest.withMcp(mcpRpc, input, id.incrementAndGet())),
            mcpRpc
        )

    operator fun invoke(mcpRpc: McpRpc, input: ClientMessage.Notification) =
        filterOut(
            mcpHandler.callWith(connectRequest.withMcp(mcpRpc, input, id.incrementAndGet())),
            mcpRpc
        )

    operator fun invoke(input: ClientMessage.Response, id: McpMessageId) {
        mcpHandler.callWith(connectRequest.withMcp(input, id)).toList()
    }

    private fun PolyHandler.callWith(request: Request): Sequence<SseMessage.Event> {
        val client = when (sessionId.get()) {
            null -> testSseClient(request)
            else -> testSseClient(request.with(Header.MCP_SESSION_ID of sessionId.get()))
        }

        val newSessionId = Header.MCP_SESSION_ID(client.response)

        if (sessionId.get() == null && newSessionId != null) {
            sessionId.set(Header.MCP_SESSION_ID(client.response))
        }

        require(client.response.status == OK)

        return client.received()
            .filterIsInstance<SseMessage.Event>().filter { it.event == "message" }
    }
}

private fun Request.withMcp(mcpRpc: McpRpc, input: ClientMessage.Request, id: Int) =
    with(McpJson) {
        method(POST)
            .accept(TEXT_EVENT_STREAM)
            .body(compact(renderRequest(mcpRpc.Method.value, asJsonObject(input), number(id))))
    }

private fun Request.withMcp(input: ClientMessage.Response, messageId: McpMessageId) =
    with(McpJson) {
        method(POST)
            .accept(TEXT_EVENT_STREAM)
            .body(compact(renderResult(asJsonObject(input), number(messageId.value))))
    }

private fun Request.withMcp(mcpRpc: McpRpc, input: ClientMessage.Notification, id: Int) =
    with(McpJson) {
        method(POST)
            .accept(TEXT_EVENT_STREAM)
            .body(compact(renderRequest(mcpRpc.Method.value, asJsonObject(input), number(id))))
    }
