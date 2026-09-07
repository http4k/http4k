/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import dev.forkhandles.time.TickableTimeSource
import java.time.Duration
import java.time.Instant

/**
 * A simulated background worker that never actually runs; its state is driven by the test via [busyUntil].
 *
 * A null [busyUntil] models a worker with no work in flight (and therefore able to stop immediately).
 */
class SimulatedThread {
    var busyUntil: Instant? = null

    fun isBusy(at: Instant): Boolean = busyUntil?.isAfter(at) ?: false
}

/**
 * An [ExecutionContext] that models the timing of a background worker without using real threads or sleeps.
 *
 * [stop] simulates waiting for in-flight work to finish, advancing the underlying [TickableTimeSource] by either
 * the time until the work completes (if within the [shutdownGracePeriod]) or by the full grace period (if not).
 * It returns whether the work finished within the grace period.
 */
class SimulatedExecutionContext(
    private val timeSource: TickableTimeSource,
    private val shutdownGracePeriod: Duration,
    val thread: SimulatedThread = SimulatedThread()
) : ExecutionContext {

    private var running = true
    var finished = false
        private set

    override fun isRunning(): Boolean = running

    override fun start(runnable: Runnable) {
        // the worker is simulated, so nothing is launched
    }

    override fun pause(duration: Duration) {
        timeSource.tick(duration)
    }

    override fun currentTime(): Instant = timeSource()

    override fun stop(): Boolean {
        running = false
        val now = currentTime()
        val busyUntil = thread.busyUntil
        val completedWithinGrace = busyUntil == null || !busyUntil.isAfter(now + shutdownGracePeriod)
        val waitFor = when {
            busyUntil == null || !busyUntil.isAfter(now) -> Duration.ZERO
            completedWithinGrace -> Duration.between(now, busyUntil)
            else -> shutdownGracePeriod
        }
        timeSource.tick(waitFor)
        finished = completedWithinGrace
        return completedWithinGrace
    }
}
