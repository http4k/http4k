package org.http4k.postbox.processing

import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

object DefaultExecutionContext : ExecutionContext {
    private var running = true
    private var executor = Executors.newVirtualThreadPerTaskExecutor()

    override fun stop() {
        running = false
        executor.shutdown()
    }

    override fun isRunning(): Boolean = running

    override fun start(runnable: Runnable) {
        executor.execute(runnable)
    }

    override fun currentTime(): Instant = Instant.now()

    override fun pause(duration: Duration) {
        Thread.sleep(duration)
    }
}
