/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import java.time.Duration
import java.time.Instant

interface ExecutionContext {
    fun isRunning(): Boolean
    fun start(runnable: Runnable)
    fun pause(duration: Duration)

    /**
     * Stop the processing cycle, allowing in-flight work to finish within the context's shutdown grace period.
     *
     * @return true if the work finished (or there was none) within the grace period; false if the grace period
     * elapsed with work still in flight and the worker was forcibly stopped.
     */
    fun stop(): Boolean

    fun currentTime(): Instant
}
