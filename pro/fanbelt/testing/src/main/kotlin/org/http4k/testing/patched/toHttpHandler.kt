/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.testing.patched

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.accept
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

fun PolyHandler.toHttpHandler(): HttpHandler {
    val streams = sse ?: return http ?: error("PolyHandler has neither http nor sse")
    val pages = http ?: { Response(NOT_ACCEPTABLE) }
    return { request ->
        val stream = if (request.accept()?.accepts(TEXT_EVENT_STREAM) == true) streams(request) else null
        if (stream?.handled == true) streamOverHttp(request, stream) else pages(request)
    }
}

private fun streamOverHttp(request: Request, sseResponse: SseResponse): Response {
    val stream = MessageStream()
    val sse = object : PushAdaptingSse(request) {
        override fun send(message: SseMessage) = apply { stream.push(message.toMessage().toByteArray()) }

        override fun close() = stream.end()
    }
    stream.onClose = sse::triggerClose
    sseResponse.consumer(sse)
    return Response(sseResponse.status).headers(sseResponse.headers)
        .with(CONTENT_TYPE of TEXT_EVENT_STREAM)
        .body(stream)
}

private class MessageStream : InputStream() {
    private val queue = LinkedBlockingQueue<ByteArray>()
    private var chunk = ByteArray(0)
    private var position = 0
    private val ended = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    var onClose: () -> Unit = {}

    fun push(bytes: ByteArray) {
        if (!ended.get()) queue.add(bytes)
    }

    fun end() {
        if (ended.compareAndSet(false, true)) queue.add(END)
    }

    override fun available() = chunk.size - position + queue.sumOf { it.size }

    override fun read() = if (refilled()) chunk[position++].toInt() and 0xff else -1

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!refilled()) return -1
        val count = minOf(length, chunk.size - position)
        System.arraycopy(chunk, position, buffer, offset, count)
        position += count
        return count
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        end()
        onClose()
    }

    private fun refilled(): Boolean {
        if (position < chunk.size) return true
        if (closed.get()) return false
        val next = queue.take()
        if (next === END) {
            queue.add(END)
            return false
        }
        chunk = next
        position = 0
        return true
    }

    private companion object {
        val END = ByteArray(0)
    }
}
