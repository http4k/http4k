package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.FixedTimeSource
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.db.InMemoryTransactor
import org.http4k.events.StdOutEvents
import org.http4k.postbox.inmemory.InMemoryPostbox
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class PostboxProcessingTest {

    @Test
    fun `process a single pending request`() {
        val timeSource = FixedTimeSource()
        val transactor = InMemoryTransactor<Postbox>(InMemoryPostbox(timeSource))
        val okResponse = { _: Request -> Response(OK) }
        val successCriteria: (Response) -> Boolean = { it.status.successful }

        val events = StdOutEvents

        transactor.perform { it.store(RequestId.of("0"), Request(Method.GET, "/")) }

        PostboxProcessing(
            transactor,
            okResponse,
            context = TestExecutionContext(timeSource, 10),
            events = events,
            successCriteria = successCriteria
        ).start()

        assertThat(transactor.perform { it.pendingRequests(10, timeSource()) }, equalTo(emptyList()))
        assertThat(
            transactor.perform { it.status(RequestId.of("0")) },
            equalTo(Success(RequestProcessingStatus.Processed(Response(OK))))
        )
    }
}

class TestExecutionContext(private val timeSource: FixedTimeSource, private val maxTicks: Int = 10) : ExecutionContext {
    private var ticks = 0
    override fun stop() {
    }

    override fun isRunning() = ticks < maxTicks

    override fun pause(duration: Duration) {
        ticks++
        timeSource.tick(duration)
    }

    override fun start(runnable: Runnable) {
        DirectExecutor.execute(runnable)
    }

    override fun currentTime(): Instant = timeSource()

    companion object {
        object DirectExecutor : AbstractExecutorService() {
            override fun execute(command: Runnable) {
                command.run()
            }

            override fun shutdown() {}
            override fun shutdownNow(): MutableList<Runnable> = mutableListOf()
            override fun isShutdown(): Boolean = false
            override fun isTerminated(): Boolean = false
            override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
        }
    }
}
