package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ChaosBehaviourTest {
    private val tx = HttpTransaction(Request(GET, ""), Response(Status.OK).body("hello"), Duration.ZERO)

    @Test(expected = ChaosException::class)
    fun `exception throwing behaviour should throw exception`() {
        ChaosBehaviour.ThrowException()(tx)
    }

    @Test
    fun `additional latency behaviour should add extra latency`() {
        val delay = 10L
        val latch = CountDownLatch(1)
        thread {
            ChaosBehaviour.Latency(
                    Duration.ofMillis(delay),
                    Duration.ofMillis(delay + 1)
            )(tx)
            latch.countDown()
        }
        assertFalse(latch.await(delay - 1, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should return response with internal server error status`() {
        val injectedResponse = ChaosBehaviour.ReturnStatus(NOT_FOUND)(tx)
        assertEquals(NOT_FOUND, injectedResponse.status)
    }

    @Test
    fun `should return no body`() {
        ChaosBehaviour.NoBody()(tx) shouldMatch hasHeader("x-http4k-chaos", "No body")
                .and(hasBody(""))
    }
}
