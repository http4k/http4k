package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class DeterministicSchedulerTest {
    private val scheduler = DeterministicScheduler(Instant.EPOCH)

    @Test
    fun `work queued from other threads while it runs is never lost`() {
        val ran = AtomicInteger()
        val queuers = (1..8).map { thread { repeat(2000) { scheduler.execute { ran.incrementAndGet() } } } }

        while (queuers.any { it.isAlive }) scheduler.runUntilIdle()
        scheduler.runUntilIdle()

        assertThat(ran.get(), equalTo(16000))
    }

    @Test
    fun `time only moves forward while another thread is running due work`() {
        scheduler.pumping().use {
            repeat(2000) { scheduler.tick(Duration.ofSeconds(1)) }
        }

        assertThat(scheduler.currentTime(), equalTo(Instant.EPOCH.plusSeconds(2000)))
    }

    @Test
    fun `due work queued from another thread runs while pumping, without anyone ticking`() {
        val ran = CountDownLatch(1)

        scheduler.pumping().use {
            thread { scheduler.execute { ran.countDown() } }

            assertThat(ran.await(5, SECONDS), equalTo(true))
        }
    }

    @Test
    fun `pumping never moves time, so later work waits for a tick`() {
        val ran = AtomicInteger()

        scheduler.pumping().use {
            scheduler.schedule({ ran.incrementAndGet() }, 1, SECONDS)
            Thread.sleep(50)
            assertThat(ran.get(), equalTo(0))

            scheduler.tick(Duration.ofSeconds(1))
            assertThat(ran.get(), equalTo(1))
        }
    }
}
