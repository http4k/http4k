package org.http4k.bridge

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.server.supportedOrNull

private const val CHUNK_SIZE = 8192

fun Response.into(response: HttpServerResponse) {
    headers.forEach { (k, v) -> if (v != null) response.putHeader(k, v) }
    response.setStatusCode(status.code).statusMessage = status.description
    when (val length = body.length) {
        null -> response.isChunked = true
        else -> response.putHeader("Content-Length", length.toString())
    }
    body.stream.use { input ->
        val chunk = ByteArray(CHUNK_SIZE)
        var read = input.read(chunk)
        while (read >= 0) {
            response.write(Buffer.buffer(chunk.copyOf(read))).blockingGet()
            read = input.read(chunk)
        }
    }
    response.end().blockingGet()
}

/**
 * Adapt the Vertx request to an http4k Request, streaming the body rather than buffering it.
 * Must be called on the event-loop thread: it pauses the request and wires the body handlers
 * synchronously so no inbound chunks are dropped before the http4k handler starts consuming.
 */
fun HttpServerRequest.asHttp4k(vertx: Vertx): Request? = Method.supportedOrNull(method().name())?.let { method ->
    val context = vertx.orCreateContext
    pause()
    val body = QueueInputStream { context.runOnContext { fetch(1) } }
    handler { body.push(it.bytes) }
    endHandler { body.end() }
    exceptionHandler { body.fail(it) }
    headers()
        .fold(Request(method, uri())) { acc, (k, v) -> acc.header(k, v) }
        .body(body, headers().get("Content-Length")?.toLongOrNull())
}

private fun <T> Future<T>.blockingGet(): T = toCompletionStage().toCompletableFuture().get()
