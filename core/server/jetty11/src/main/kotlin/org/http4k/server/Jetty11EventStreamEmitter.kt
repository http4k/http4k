package org.http4k.server

import org.eclipse.jetty.util.component.LifeCycle
import org.eclipse.jetty.util.thread.AutoLock
import org.eclipse.jetty.util.thread.Scheduler
import org.http4k.core.Request
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseMessage
import java.io.IOException
import java.io.OutputStream
import java.time.Duration
import java.util.concurrent.TimeUnit

class Jetty11EventStreamEmitter(
    connectRequest: Request,
    private val output: OutputStream,
    private val heartBeatDuration: Duration,
    private val scheduler: Scheduler,
    private val onClose: (Jetty11EventStreamEmitter) -> Unit
) : PushAdaptingSse(connectRequest), Runnable, LifeCycle.Listener {
    private val lock: AutoLock = AutoLock()
    private var heartBeat: Scheduler.Task? = null
    private var closed = false

    init {
        scheduleHeartBeat()
    }

    override fun send(message: SseMessage)  = apply {
        when (message) {
            is SseMessage.Event -> sendEvent(message.event, message.data, message.id)
            is SseMessage.Data -> sendData(message.data)
            is SseMessage.Retry -> sendRetry(message.backoff)
        }
    }

    private fun sendEvent(event: String, data: String, id: String?) = lock.lock().use {
        id?.also {
            output.write(ID_FIELD)
            output.write(it.toByteArray())
            output.write(DELIMITER)
        }
        output.write(EVENT_FIELD)
        output.write(event.toByteArray())
        output.write(DELIMITER)
        sendData(data)
    }

    private fun sendData(data: String) = lock.lock().use {
        data.lines().forEach { line ->
            output.write(DATA_FIELD)
            output.write(line.toByteArray())
            output.write(DELIMITER)
        }
        output.write(DELIMITER)
        output.flush()
    }

    private fun sendRetry(duration: Duration) = lock.lock().use {
        output.write(RETRY_FIELD)
        output.write(duration.toMillis().toString().toByteArray())
        output.write(DELIMITER)
        output.write(DELIMITER)
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
        private val DELIMITER = "\n".toByteArray()
        private val ID_FIELD = "id:".toByteArray()
        private val EVENT_FIELD = "event:".toByteArray()
        private val DATA_FIELD = "data:".toByteArray()
        private val RETRY_FIELD = "retry:".toByteArray()
    }
}
