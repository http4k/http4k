/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.infrastructure

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class VirtualThreadSchedulerTest {
    @Test
    fun `a cancelled task is let go at once, not held until it would have run`() {
        val scheduler = VirtualThreadScheduler() as ScheduledThreadPoolExecutor

        repeat(1000) { scheduler.schedule({}, 7, DAYS).cancel(false) }

        assertThat(scheduler.queue.size, equalTo(0))
        scheduler.close()
    }

    @Test
    fun `a task still runs while a thousand others are blocked, so a heartbeat isn't starved by stuck redraws or writes`() {
        val scheduler = VirtualThreadScheduler()
        val released = CountDownLatch(1)
        repeat(1000) { scheduler.execute { released.await() } }

        val ran = CountDownLatch(1)
        scheduler.schedule({ ran.countDown() }, 10, MILLISECONDS)

        assertThat(ran.await(5, SECONDS), equalTo(true))
        released.countDown()
        scheduler.close()
    }
}
