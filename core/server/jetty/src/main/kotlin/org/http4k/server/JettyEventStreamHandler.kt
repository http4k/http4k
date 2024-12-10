package org.http4k.server

import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.http.HttpHeaderValue
import org.eclipse.jetty.io.Content
import org.eclipse.jetty.server.Handler.Abstract
import org.eclipse.jetty.util.Callback
import org.eclipse.jetty.util.component.LifeCycle
import org.eclipse.jetty.util.thread.AutoLock
import org.eclipse.jetty.util.thread.Scheduler
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Headers
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import java.io.IOException
import java.io.OutputStream
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.eclipse.jetty.server.Request as JettyRequest
import org.eclipse.jetty.server.Response as JettyResponse

class JettyEventStreamHandler(
    private val sse: SseHandler,
    private val heartBeatDuration: Duration = Duration.ofSeconds(15)
) : Abstract.NonBlocking() {

    override fun handle(request: JettyRequest, response: JettyResponse, callback: Callback) =
        when {
            request.isEventStream() -> {
                val connectRequest = request.asHttp4kRequest()
                when {
                    connectRequest != null -> {
                        with(sse(connectRequest)) {
                            when {
                                handled -> {
                                    response.writeEventStreamResponse(status, headers).handle { _, flushFailure ->
                                        if (flushFailure == null) {
                                            val output = Content.Sink.asOutputStream(response)
                                            val scheduler = request.connectionMetaData.connector.scheduler
                                            val server = request.connectionMetaData.connector.server

                                            consumer(
                                                JettyEventStreamEmitter(connectRequest,
                                                    output,
                                                    heartBeatDuration,
                                                    scheduler,
                                                    onClose = { emitter, emitterFailure ->
                                                        if (emitterFailure == null) {
                                                            callback.succeeded()
                                                        } else {
                                                            callback.failed(emitterFailure)
                                                        }
                                                        server.removeEventListener(emitter)
                                                    }
                                                ).also(server::addEventListener)
                                            )
                                        } else {
                                            callback.failed(flushFailure)
                                        }
                                    }
                                }
                            }
                            handled
                        }
                    }

                    else -> false
                }
            }
            // Not a valid event stream request - return false and let next handler process the request
            else -> false
        }

    companion object {
        private fun JettyRequest.isEventStream() = headers[HttpHeader.ACCEPT].equals(TEXT_EVENT_STREAM.value, true)

        private fun JettyResponse.writeEventStreamResponse(
            newStatus: Status,
            additionalHeaders: Headers
        ): Callback.Completable {
            status = newStatus.code
            headers.add(HttpHeader.CONTENT_TYPE, TEXT_EVENT_STREAM.toHeaderValue())
            // By adding this header, and not closing the connection,
            // we disable HTTP chunking, and we can use write()+flush()
            // to send data in the text/event-stream protocol
            headers.add(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE)
            additionalHeaders.forEach { (key, value) -> headers.add(key, value) }

            return Callback.Completable.with { flushCallback ->
                write(false, null, flushCallback)
            }
        }
    }
}

internal class JettyEventStreamEmitter(
    connectRequest: Request,
    private val output: OutputStream,
    private val heartBeatDuration: Duration,
    private val scheduler: Scheduler,
    private val onClose: (JettyEventStreamEmitter, Throwable?) -> Unit
) : PushAdaptingSse(connectRequest), Runnable, LifeCycle.Listener {
    private val lock: AutoLock = AutoLock()
    private var heartBeat: Scheduler.Task? = null
    private var closed = false

    init {
        scheduleHeartBeat()
    }

    override fun send(message: SseMessage) = apply {
        when (message) {
            is SseMessage.Event -> sendEvent(message.event, message.data, message.id)
            is SseMessage.Data -> sendData(message.data)
            is SseMessage.Retry -> sendRetry(message.backoff)
        }
    }

    private fun sendEvent(event: String, data: String, id: String?) = lock.lock().use {
        safeOutput {
            id?.also {
                write(ID_FIELD)
                write(it.toByteArray())
                write(DELIMITER)
            }
            write(EVENT_FIELD)
            write(event.toByteArray())
            write(DELIMITER)
            sendData(data)
        }
    }

    private fun sendData(data: String) = lock.lock().use {
        safeOutput {
            data.lines().forEach { line ->
                write(DATA_FIELD)
                write(line.toByteArray())
                write(DELIMITER)
            }
            write(DELIMITER)
            flush()
        }
    }

    private fun sendRetry(duration: Duration) = lock.lock().use {
        safeOutput {
            write(RETRY_FIELD)
            write(duration.toMillis().toString().toByteArray())
            write(DELIMITER)
            write(DELIMITER)
            flush()
        }
    }

    override fun close() = lock.lock().use {
        doClose(null)
    }

    private fun doClose(failure: Throwable?) {
        if (!closed) {
            closed = true
            heartBeat?.cancel()
            onClose(this, failure)
            triggerClose()
        }
    }

    override fun lifeCycleStopping(event: LifeCycle) {
        close()
    }

    override fun run() {
        safeOutput {
            // If the other peer closes the connection, the first
            // flush() should generate a TCP reset that is detected
            // on the second flush()
            lock.lock().use {
                write('\r'.code)
                flush()
                write('\n'.code)
                flush()
            }
            scheduleHeartBeat()
        }
    }

    private fun safeOutput(block: OutputStream.() -> Unit) {
        try {
            block(output)
        } catch (e: IOException) {
            // Close connection on write failure
            doClose(e)
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
        private val DELIMITER = "\n".toByteArray()
        private val ID_FIELD = "id:".toByteArray()
        private val EVENT_FIELD = "event:".toByteArray()
        private val DATA_FIELD = "data:".toByteArray()
        private val RETRY_FIELD = "retry:".toByteArray()
    }
}
