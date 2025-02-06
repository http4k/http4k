package org.http4k.postbox.processing

import java.time.Duration
import java.time.Instant

interface ExecutionContext {
    fun isRunning(): Boolean
    fun start(runnable: Runnable)
    fun pause(duration: Duration)
    fun stop()
    fun currentTime(): Instant
}
