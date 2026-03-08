/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.hotreload

import java.time.Duration

interface TaskRunner {
    operator fun <T> invoke(fn: () -> T): T

    companion object {
        fun retry(maxAttempts: Int = 5, sleep: Duration = Duration.ofMillis(100)) = object : TaskRunner {
            override operator fun <T> invoke(fn: () -> T): T {
                var lastException: Exception? = null
                for (attempt in 1..maxAttempts) {
                    try {
                        return fn()
                    } catch (e: Exception) {
                        lastException = e
                        if (attempt == maxAttempts) break;
                        Thread.sleep(sleep)
                    }
                }
                throw lastException!!
            }
        }
    }
}
