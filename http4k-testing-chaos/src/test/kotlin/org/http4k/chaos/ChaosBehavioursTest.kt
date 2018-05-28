package org.http4k.chaos

import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ChaosBehavioursTest {
    @Test(expected = ChaosException::class)
    fun `exception throwing behaviour should throw exception`() {
        ExceptionThrowingBehaviour().inject(Request(Method.GET, ""))
    }

    @Test
    fun `additional latency behaviour should add extra latency`() {
        val delay = 100L
        val latch = CountDownLatch(1)
        thread {
            AdditionalLatencyBehaviour(
                Duration.ofMillis(delay),
                Duration.ofMillis(delay + 1)
            ).inject(Request(Method.GET, ""))
            latch.countDown()
        }
        assertFalse(latch.await(delay - 1, TimeUnit.MILLISECONDS))
    }
}
