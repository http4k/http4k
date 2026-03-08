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
    fun stop()
    fun currentTime(): Instant
}
