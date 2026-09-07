/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [ExecutionContext] that runs its polling cycle on a background (virtual) thread and which, on [stop], waits for
 * in-flight work to finish for up to [shutdownGracePeriod] before giving up.
 */
class DefaultExecutionContext(
    private val shutdownGracePeriod: Duration = Duration.ofSeconds(30)
) : ExecutionContext {

    private val lock = Any()
    private val running = AtomicBoolean(true)

    @Volatile
    private var executor = newExecutor()

    @Volatile
    private var pausedThread: Thread? = null

    override fun start(runnable: Runnable) {
        synchronized(lock) {
            if (executor.isShutdown) executor = newExecutor()
            running.set(true)
            executor.execute(runnable)
        }
    }

    override fun stop(): Boolean {
        if (running.getAndSet(false)) {
            executor.shutdown()
            pausedThread?.interrupt()
            return try {
                executor.awaitTermination(shutdownGracePeriod.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        }
        return true
    }

    override fun isRunning(): Boolean = running.get()

    override fun currentTime(): Instant = Instant.now()

    override fun pause(duration: Duration) {
        pausedThread = Thread.currentThread()
        try {
            if (running.get()) {
                Thread.sleep(duration.toMillis())
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            pausedThread = null
        }
    }
}

private fun newExecutor() = Executors.newVirtualThreadPerTaskExecutor()
