package org.http4k.server

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Headers
import org.http4k.core.Status
import org.http4k.servlet.jakarta.asHttp4kRequest
import org.http4k.sse.SseHandler
import java.nio.charset.StandardCharsets
import java.time.Duration
import org.eclipse.jetty.server.Request as JettyRequest

class Jetty11EventStreamHandler(
    private val sse: SseHandler,
    private val heartBeatDuration: Duration = Duration.ofSeconds(15)
) : HandlerWrapper() {
    override fun handle(
        target: String, baseRequest: JettyRequest,
        request: HttpServletRequest, response: HttpServletResponse
    ) {
        if (!baseRequest.isHandled && request.isEventStream()) {
            val connectRequest = request.asHttp4kRequest()
            when {
                connectRequest != null -> {
                    with(sse(connectRequest)) {

                        if (handled) {
                            response.writeEventStreamResponse(status, headers)

                            val async = request.startAsyncWithNoTimeout()
                            val output = async.response.outputStream
                            val scheduler = baseRequest.httpChannel.connector.scheduler
                            val server = baseRequest.httpChannel.connector.server

                            val emitter =
                                Jetty11EventStreamEmitter(
                                    connectRequest,
                                    output,
                                    heartBeatDuration,
                                    scheduler,
                                    onClose = {
                                        async.complete()
                                        server.removeEventListener(it)
                                    }).also(server::addEventListener)
                            consumer(emitter)
                        }

                        baseRequest.isHandled = handled
                    }
                }
            }
        }

        if (!baseRequest.isHandled) super.handle(target, baseRequest, request, response)
    }

    companion object {
        private fun HttpServletRequest.isEventStream() =
            getHeaders("Accept").toList().any { it.contains(TEXT_EVENT_STREAM.value, true) }

        private fun HttpServletResponse.writeEventStreamResponse(newStatus: Status, headers: Headers) {
            status = newStatus.code
            characterEncoding = StandardCharsets.UTF_8.name()
            contentType = TEXT_EVENT_STREAM.value
            // By adding this header, and not closing the connection,
            // we disable HTTP chunking, and we can use write()+flush()
            // to send data in the text/event-stream protocol
            addHeader("Connection", "close")
            headers.forEach { addHeader(it.first, it.second) }
            flushBuffer()
        }

        private fun HttpServletRequest.startAsyncWithNoTimeout() =
            startAsync().apply {
                // Infinite timeout because the continuation is never resumed,
                // but only completed on close
                timeout = 0
            }
    }
}
