package org.http4k.chaos

import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ChaosBehaviourTest {
    @Test(expected = ChaosException::class)
    fun `exception throwing behaviour should throw exception`() {
        ChaosBehaviour.ThrowException().inject(Response(Status.OK))
    }

    @Test
    fun `additional latency behaviour should add extra latency`() {
        val delay = 10L
        val latch = CountDownLatch(1)
        thread {
            ChaosBehaviour.Latency(
                Duration.ofMillis(delay),
                Duration.ofMillis(delay + 1)
            ).inject(Response(Status.OK))
            latch.countDown()
        }
        assertFalse(latch.await(delay - 1, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should return response with internal server error status`() {
        val response = Response(Status.OK)
        val injectedResponse = ChaosBehaviour.ReturnStatus().inject(response)
        assertEquals(Status.INTERNAL_SERVER_ERROR, injectedResponse.status)
    }
}
