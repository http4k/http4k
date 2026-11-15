/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.infrastructure

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit.DAYS

class VirtualThreadSchedulerTest {
    @Test
    fun `a cancelled task is let go at once, not held until it would have run`() {
        val scheduler = VirtualThreadScheduler() as ScheduledThreadPoolExecutor

        repeat(1000) { scheduler.schedule({}, 7, DAYS).cancel(false) }

        assertThat(scheduler.queue.size, equalTo(0))
        scheduler.close()
    }
}
