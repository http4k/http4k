package org.http4k.server

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.util.component.LifeCycle
import org.eclipse.jetty.util.thread.AutoLock
import org.eclipse.jetty.util.thread.Scheduler
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Headers
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.servlet.jakarta.asHttp4kRequest
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.eclipse.jetty.server.Request as JettyRequest

class JettyEventStreamHandler(
    private val sse: SseHandler,
    private val heartBeatDuration: Duration = Duration.ofSeconds(15)
) : HandlerWrapper() {

    override fun handle(
        target: String, baseRequest: JettyRequest,
        request: HttpServletRequest, response: HttpServletResponse
    ) {
        if (!baseRequest.isHandled && request.isEventStream()) {
            val connectRequest = request.asHttp4kRequest()
            if (connectRequest != null) {
                val (status, headers, consumer) = sse(connectRequest)
                response.writeEventStreamResponse(status, headers)

                val async = request.startAsyncWithNoTimeout()
                val output = async.response.outputStream
                val scheduler = baseRequest.httpChannel.connector.scheduler
                val server = baseRequest.httpChannel.connector.server

                val emitter = JettyEventStreamEmitter(connectRequest, output, heartBeatDuration, scheduler, onClose = {
                    async.complete()
                    server.removeEventListener(it)
                }).also(server::addEventListener)
                consumer(emitter)

                baseRequest.isHandled = true
            }
        }

        if (!baseRequest.isHandled) super.handle(target, baseRequest, request, response)
    }

    companion object {
        private fun HttpServletRequest.isEventStream() =
            method == "GET" && getHeaders("Accept").toList().any { it.contains(TEXT_EVENT_STREAM.value) }

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

internal class JettyEventStreamEmitter(
    connectRequest: Request,
    private val output: OutputStream,
    private val heartBeatDuration: Duration,
    private val scheduler: Scheduler,
    private val onClose: (JettyEventStreamEmitter) -> Unit
) : PushAdaptingSse(connectRequest), Runnable, LifeCycle.Listener {
    private val lock: AutoLock = AutoLock()
    private var heartBeat: Scheduler.Task? = null
    private var closed = false

    init {
        scheduleHeartBeat()
    }

    override fun send(message: SseMessage) = when (message) {
        is SseMessage.Event -> sendEvent(message.event, message.data, message.id)
        is SseMessage.Data -> sendData(message.data)
        is SseMessage.Retry -> sendRetry(message.backoff)
    }

    private fun sendEvent(event: String, data: String, id: String?) = lock.lock().use {
        id?.also {
            output.write(ID_FIELD)
            output.write(it.toByteArray())
            output.write(CRLF)
        }
        output.write(EVENT_FIELD)
        output.write(event.toByteArray())
        output.write(CRLF)
        sendData(data)
    }

    private fun sendData(data: String) = lock.lock().use {
        data.lines().forEach { line ->
            output.write(DATA_FIELD)
            output.write(line.toByteArray())
            output.write(CRLF)
        }
        output.write(CRLF)
        output.flush()
    }

    private fun sendRetry(duration: Duration) = lock.lock().use {
        output.write(RETRY_FIELD)
        output.write(duration.toMillis().toString().toByteArray())
        output.write(CRLF)
        output.write(CRLF)
        output.flush()
    }

    override fun close() = lock.lock().use {
        if (!closed) {
            closed = true
            heartBeat?.cancel()
            onClose(this)
            triggerClose()
        }
    }

    override fun lifeCycleStopping(event: LifeCycle) {
        close()
    }

    override fun run() {
        try {
            // If the other peer closes the connection, the first
            // flush() should generate a TCP reset that is detected
            // on the second flush()
            lock.lock().use {
                output.write('\r'.code)
                output.flush()
                output.write('\n'.code)
                output.flush()
            }
            scheduleHeartBeat()
        } catch (e: IOException) {
            // The other peer closed the connection
            close()
        }
    }

    private fun scheduleHeartBeat() {
        lock.lock().use {
            if (!closed) {
                heartBeat = scheduler.schedule(this, heartBeatDuration.toMillis(), TimeUnit.MILLISECONDS)
            }
        }
    }

    companion object {
        private val CRLF = "\r\n".toByteArray()
        private val ID_FIELD = "id:".toByteArray()
        private val EVENT_FIELD = "event:".toByteArray()
        private val DATA_FIELD = "data:".toByteArray()
        private val RETRY_FIELD = "retry:".toByteArray()
    }
}
