package org.http4k.server

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.sameInstance
import org.eclipse.jetty.util.component.AbstractLifeCycle
import org.eclipse.jetty.util.thread.Scheduler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class JettyEventStreamEmitterTest {

    @Test
    fun `sse connectRequest is returned`() {
        val emitter = JettyEventStreamEmitter(connectRequest, FakeOutput(), Duration.ofMillis(5), FakeScheduler(), onClose = {})

        assertThat(emitter.connectRequest, equalTo(connectRequest))
    }

    @Test
    fun `can send Retry message`() {
        val output = FakeOutput()
        val emitter = JettyEventStreamEmitter(connectRequest, output, Duration.ofMillis(5), FakeScheduler(), onClose = {})

        emitter.send(SseMessage.Retry(Duration.ofMillis(3)))

        assertThat(output.toString(), equalTo("retry:3\r\n\r\n"))
        assertThat(output.flushCalls, equalTo(1))
    }

    @Test
    fun `can send Data message`() {
        val output = FakeOutput()
        val emitter = JettyEventStreamEmitter(connectRequest, output, Duration.ofMillis(5), FakeScheduler(), onClose = {})

        emitter.send(SseMessage.Data("some data"))

        assertThat(output.toString(), equalTo("data:some data\r\n\r\n"))
        assertThat(output.flushCalls, equalTo(1))
    }

    @Test
    fun `can send Data message containing multiple lines`() {
        val output = FakeOutput()
        val emitter = JettyEventStreamEmitter(connectRequest, output, Duration.ofMillis(5), FakeScheduler(), onClose = {})

        emitter.send(SseMessage.Data("some data\nwith another line"))

        assertThat(output.toString(), equalTo("data:some data\r\ndata:with another line\r\n\r\n"))
        assertThat(output.flushCalls, equalTo(1))
    }

    @Test
    fun `can send Event message with an id`() {
        val output = FakeOutput()
        val emitter = JettyEventStreamEmitter(connectRequest, output, Duration.ofMillis(5), FakeScheduler(), onClose = {})

        emitter.send(SseMessage.Event("event name", "some data", "an id"))

        assertThat(output.toString(), equalTo("id:an id\r\nevent:event name\r\ndata:some data\r\n\r\n"))
        assertThat(output.flushCalls, equalTo(1))
    }

    @Test
    fun `can send Event message without an id`() {
        val output = FakeOutput()
        val emitter = JettyEventStreamEmitter(connectRequest, output, Duration.ofMillis(5), FakeScheduler(), onClose = {})

        emitter.send(SseMessage.Event("event name", "some data", id = null))

        assertThat(output.toString(), equalTo("event:event name\r\ndata:some data\r\n\r\n"))
        assertThat(output.flushCalls, equalTo(1))
    }

    @Test
    fun `can send Event message with multiline data`() {
        val output = FakeOutput()
        val emitter = JettyEventStreamEmitter(connectRequest, output, Duration.ofMillis(5), FakeScheduler(), onClose = {})

        emitter.send(SseMessage.Event("event name", "some data\nwith another line", id = null))

        assertThat(output.toString(), equalTo("event:event name\r\ndata:some data\r\ndata:with another line\r\n\r\n"))
        assertThat(output.flushCalls, equalTo(1))
    }

    @Test
    fun `schedules a heart beat on creation with provided duration`() {
        val scheduler = FakeScheduler()
        val emitter = JettyEventStreamEmitter(connectRequest, FakeOutput(), Duration.ofMillis(5), scheduler, onClose = {})

        assertThat(scheduler.currentTask, present(allOf(
            has(FakeScheduler.FakeTask::task, sameInstance(emitter)),
            has(FakeScheduler.FakeTask::delay, equalTo(5)),
            has(FakeScheduler.FakeTask::units, equalTo(TimeUnit.MILLISECONDS))
        )))
        assertThat(scheduler.tasksScheduled, equalTo(1))
    }

    @Test
    fun `when a heart beat triggers a blank line is written to the output and a new heart beat is scheduled`() {
        val scheduler = FakeScheduler()
        val output = FakeOutput()
        JettyEventStreamEmitter(connectRequest, output, Duration.ofMillis(5), scheduler, onClose = {})

        scheduler.trigger()

        assertThat(output.toString(), equalTo("\r\n"))
        assertThat(output.flushCalls, equalTo(2))
        assertThat(scheduler.tasksScheduled, equalTo(2))
    }

    @Test
    fun `close cancels the heart beat and invokes the onClose callbacks`() {
        val scheduler = FakeScheduler()
        val emitterOnCloseCallCount = AtomicInteger(0)
        val sseOnCloseCallCount = AtomicInteger(0)
        val emitter = JettyEventStreamEmitter(connectRequest, FakeOutput(), Duration.ofMillis(5), scheduler,
            onClose = { emitterOnCloseCallCount.incrementAndGet() })
        emitter.onClose { sseOnCloseCallCount.incrementAndGet() }

        emitter.close()

        assertThat(scheduler.currentTask, present(has(FakeScheduler.FakeTask::cancelled, equalTo(true))))
        assertThat(emitterOnCloseCallCount.get(), equalTo(1))
        assertThat(sseOnCloseCallCount.get(), equalTo(1))
    }

    @Test
    fun `close called multiple times only cancels the heart beat and invokes the onClose callbacks once`() {
        val scheduler = FakeScheduler()
        val emitterOnCloseCallCount = AtomicInteger(0)
        val sseOnCloseCallCount = AtomicInteger(0)
        val emitter = JettyEventStreamEmitter(connectRequest, FakeOutput(), Duration.ofMillis(5), scheduler,
            onClose = { emitterOnCloseCallCount.incrementAndGet() })
        emitter.onClose { sseOnCloseCallCount.incrementAndGet() }

        emitter.close()
        emitter.close()
        emitter.close()

        assertThat(scheduler.currentTask, present(has(FakeScheduler.FakeTask::cancelled, equalTo(true))))
        assertThat(emitterOnCloseCallCount.get(), equalTo(1))
        assertThat(sseOnCloseCallCount.get(), equalTo(1))
    }

    @Test
    fun `close is called on lifeCycleStopping`() {
        val scheduler = FakeScheduler()
        val emitterOnCloseCallCount = AtomicInteger(0)
        val sseOnCloseCallCount = AtomicInteger(0)
        val emitter = JettyEventStreamEmitter(connectRequest, FakeOutput(), Duration.ofMillis(5), scheduler,
            onClose = { emitterOnCloseCallCount.incrementAndGet() })
        emitter.onClose { sseOnCloseCallCount.incrementAndGet() }

        emitter.lifeCycleStopping(object : AbstractLifeCycle() {})

        assertThat(scheduler.currentTask, present(has(FakeScheduler.FakeTask::cancelled, equalTo(true))))
        assertThat(emitterOnCloseCallCount.get(), equalTo(1))
        assertThat(sseOnCloseCallCount.get(), equalTo(1))
    }

    @Test
    fun `close is called when the heart beat cannot write to the output`() {
        val scheduler = FakeScheduler()
        val emitterOnCloseCallCount = AtomicInteger(0)
        val sseOnCloseCallCount = AtomicInteger(0)
        val emitter = JettyEventStreamEmitter(connectRequest, FakeOutput(flushFails = true), Duration.ofMillis(5), scheduler,
            onClose = { emitterOnCloseCallCount.incrementAndGet() })
        emitter.onClose { sseOnCloseCallCount.incrementAndGet() }

        scheduler.trigger()

        assertThat(scheduler.currentTask, present(has(FakeScheduler.FakeTask::cancelled, equalTo(true))))
        assertThat(emitterOnCloseCallCount.get(), equalTo(1))
        assertThat(sseOnCloseCallCount.get(), equalTo(1))
    }

    companion object {
        private val connectRequest = Request(Method.GET, "/some/path")
    }
}

private class FakeOutput(private val flushFails: Boolean = false) : ByteArrayOutputStream() {
    var flushCalls = 0

    override fun flush() {
        if (flushFails) {
            throw IOException()
        }
        flushCalls++
        super.flush()
    }
}

private class FakeScheduler : Scheduler, AbstractLifeCycle() {
    var currentTask: FakeTask? = null
    var tasksScheduled = 0

    override fun schedule(task: Runnable, delay: Long, units: TimeUnit): Scheduler.Task {
        return FakeTask(task, delay, units).also {
            currentTask = it
            tasksScheduled++
        }
    }

    fun trigger() {
        currentTask?.task?.run()
    }

    class FakeTask(val task: Runnable, val delay: Long, val units: TimeUnit) : Scheduler.Task {
        var cancelled = false

        override fun cancel(): Boolean {
            require(!cancelled) {
                "Task has already been cancelled"
            }
            cancelled = true
            return true
        }
    }
}
