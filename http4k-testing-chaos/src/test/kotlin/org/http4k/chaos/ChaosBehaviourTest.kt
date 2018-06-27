package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.chaos.ChaosBehaviour.Companion.Latency
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration.ZERO
import java.time.Duration.ofMillis
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.concurrent.thread

class ChaosBehaviourTest {
    private val tx = HttpTransaction(Request(GET, ""), Response(Status.OK).body("hello"), ZERO)

    @Test
    fun `exception throwing behaviour should throw exception`() {
        val expected = RuntimeException("foo")
        assertThat({ ChaosBehaviour.ThrowException(expected)(tx) }, throws(equalTo(expected)))

    }

    @Test
    @Disabled
    fun `additional latency behaviour should add extra latency`() {
        val delay = 10L
        val latch = CountDownLatch(1)
        thread {
            Latency(ofMillis(delay)..ofMillis(delay + 1))(tx)
            latch.countDown()
        }
        assertThat(latch.await(delay - 1, MILLISECONDS), equalTo(false))
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

    @Test
    fun `should block thread`() {
        val latch = CountDownLatch(1)
        thread {
            ChaosBehaviour.BlockThread()(tx)
            latch.countDown()
        }

        assertThat(latch.await(100, MILLISECONDS), equalTo(false))
    }

    @Test
    fun `should eat memory`() {
        assertThat({ ChaosBehaviour.EatMemory()(tx) }, throws<OutOfMemoryError>())
    }

    @Test
    @Disabled // untestable
    fun `should stack overflow`() {
        ChaosBehaviour.StackOverflow()(tx)
    }

    @Test
    @Disabled // untestable
    fun `should kill process`() {
        ChaosBehaviour.KillProcess()(tx)
    }
}
