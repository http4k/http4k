package org.http4k.bridge

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.server.supportedOrNull
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Blocking.get
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.stream.Streams
import java.io.InputStream

private const val CHUNK_SIZE = 8192

fun RatpackToHttp4kHandler(httpHandler: HttpHandler) = Handler { context ->
    val request = context.toHttp4kRequest()
    get { request?.let(httpHandler) ?: Response(NOT_IMPLEMENTED) }
        .then { it.streamTo(context) }
}

/**
 * Adapt the Ratpack request, streaming the body rather than buffering it: the request's body
 * publisher feeds a [QueueInputStream] that the (blocking) http4k handler pulls from on demand.
 */
private fun Context.toHttp4kRequest(): Request? = Method.supportedOrNull(request.method.name)?.let { method ->
    val subscriber = RequestBodySubscriber()
    request.bodyStream.subscribe(subscriber)
    request.headers.names
        .fold(Request(method, request.rawUri, request.protocol)) { acc, name ->
            request.headers.getAll(name).fold(acc) { withValues, value -> withValues.header(name, value) }
        }
        .body(subscriber.body, request.headers.get("Content-Length")?.toLongOrNull())
        .source(RequestSource(request.remoteAddress.host, request.remoteAddress.port))
}

private fun Response.streamTo(context: Context) {
    headers.groupBy { it.first }.forEach { (name, values) ->
        context.response.headers.set(name, values.mapNotNull { it.second })
    }
    context.response.status(status.code)

    val stream = body.stream
    @Suppress("UNCHECKED_CAST")
    context.response.sendStream(
        Streams.flatYield { get { readChunk(stream) } } as Publisher<ByteBuf>
    )
}

private fun readChunk(stream: InputStream): ByteBuf? {
    val buffer = ByteArray(CHUNK_SIZE)
    val read = stream.read(buffer)
    return if (read < 0) {
        stream.close()
        null
    } else Unpooled.copiedBuffer(buffer, 0, read)
}

private class RequestBodySubscriber : Subscriber<ByteBuf> {
    private var subscription: Subscription? = null
    private var pendingDemand = 0L
    val body = QueueInputStream { requestOne() }

    @Synchronized
    private fun requestOne() {
        subscription?.request(1) ?: pendingDemand++
    }

    @Synchronized
    override fun onSubscribe(s: Subscription) {
        subscription = s
        if (pendingDemand > 0) {
            s.request(pendingDemand)
            pendingDemand = 0
        }
    }

    override fun onNext(buf: ByteBuf) = body
        .push(ByteArray(buf.readableBytes()).apply {
            buf.readBytes(this)
            buf.release()
        })

    override fun onComplete() = body.end()

    override fun onError(t: Throwable) = body.fail(t)
}
