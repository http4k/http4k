/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing

import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.json
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.accept
import org.http4k.sse.SseMessage
import org.http4k.testing.testSseClient
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread


class TestMcpSender(private val mcpHandler: PolyHandler, private val connectRequest: Request) {

    private val outbound = mutableMapOf<McpRpcMethod, MutableList<(SseMessage.Event) -> Unit>>()
    private val streamEvents = CopyOnWriteArrayList<SseMessage.Event>()
    private val newEvent = Semaphore(0)
    private var streamThread: Thread? = null

    fun on(method: McpRpcMethod, fn: (SseMessage.Event) -> Unit) {
        outbound.getOrPut(method) { mutableListOf() }.add(fn)
    }

    fun startEventStream() {
        val connected = CountDownLatch(1)
        streamThread = thread(isDaemon = true) {
            val events = mcpHandler.callWith(connectRequest.accept(TEXT_EVENT_STREAM).method(GET))
            connected.countDown()
            events.forEach { event ->
                outbound[event.mcpMethod()]?.forEach { it(event) }
                streamEvents.add(event)
                newEvent.release()
            }
        }
        connected.await(5, SECONDS)
    }

    fun lastEvent(): SseMessage.Event {
        newEvent.drainPermits()
        newEvent.tryAcquire(5, SECONDS)
        return streamEvents.last()
    }

    fun stopEventStream() {
        streamThread?.interrupt()
    }

    private fun filterOut(events: Sequence<SseMessage.Event>, method: McpRpcMethod) = events
        .filter {
            when {
                it.mcpMethod() == method || it.isResult() || it.isError() -> true
                else -> {
                    outbound[it.mcpMethod()]?.forEach { sub -> sub(it) }
                    false
                }
            }
        }

    private fun SseMessage.Event.isResult() = McpJson.fields(McpJson.parse(data)).toMap().containsKey("result")
    private fun SseMessage.Event.isError() = McpJson.fields(McpJson.parse(data)).toMap().containsKey("error")

    private fun SseMessage.Event.mcpMethod() =
        McpJson.fields(McpJson.parse(data)).toMap()["method"]?.let { McpRpcMethod.of(McpJson.text(it)) }

    private var id = AtomicInteger(0)

    var sessionId = AtomicReference<SessionId>()

    fun nextId(): Int = id.incrementAndGet()

    operator fun invoke(input: McpJsonRpcRequest) =
        filterOut(
            mcpHandler.callWith(connectRequest.withMcp(input)),
            input.method
        )

    operator fun invoke(input: McpJsonRpcMessage) {
        mcpHandler.callWith(connectRequest.withMcp(input)).toList()
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

private fun Request.withMcp(input: McpJsonRpcMessage) =
    method(POST)
        .accept(TEXT_EVENT_STREAM)
        .json(input)
