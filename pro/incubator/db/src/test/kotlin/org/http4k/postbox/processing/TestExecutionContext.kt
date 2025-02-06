package org.http4k.postbox.processing

import dev.forkhandles.time.FixedTimeSource
import java.time.Duration
import java.time.Instant
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class TestExecutionContext(private val timeSource: FixedTimeSource, private val maxTicks: Int = 10) : ExecutionContext {
    private var ticks = 0
    override fun stop() {
    }

    override fun isRunning() = ticks < maxTicks

    override fun pause(duration: Duration) {
        ticks++
        timeSource.tick(duration)
        println("ts=${timeSource()}")
    }

    override fun start(runnable: Runnable) {
        DirectExecutor.execute(runnable)
    }

    override fun currentTime(): Instant = timeSource()

    override fun random(max: Int): Int = 0

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
