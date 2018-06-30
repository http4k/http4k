package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.chaos.ChaosBehaviour.Companion.Latency
import org.http4k.chaos.ChaosBehaviour.Companion.NoBody
import org.http4k.chaos.ChaosBehaviour.Companion.Variable
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
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.concurrent.thread

class ChaosBehaviourTest {
    private val tx = HttpTransaction(Request(GET, ""), Response(Status.OK).body("hello"), ZERO)

    @Test
    fun `exception throwing behaviour should throw exception`() {
        val expected = RuntimeException("foo")
        val throwException = ChaosBehaviour.ThrowException(expected)
        throwException.toString() shouldMatch equalTo(("ThrowException RuntimeException foo"))

        assertThat({ throwException(tx) }, throws(equalTo(expected)))
    }

    @Test
    @Disabled
    fun `additional latency behaviour should add extra latency`() {
        val delay = 10L
        val latency = Latency(ofMillis(delay)..ofMillis(delay + 1))
        latency.toString() shouldMatch equalTo((""))

        val latch = CountDownLatch(1)
        thread {
            latency(tx)
            latch.countDown()
        }
        assertThat(latch.await(delay - 1, MILLISECONDS), equalTo(false))
    }

    @Test
    fun `latency description`() {
        val props = Properties().apply {
            put("CHAOS_LATENCY_MS_MIN", "1000")
            put("CHAOS_LATENCY_MS_MAX", "1000000")
        }

        Latency.fromEnv(props::getProperty).toString() shouldMatch equalTo(("Latency (range = PT1S to PT1S)"))
        Latency.fromEnv().toString() shouldMatch equalTo(("Latency (range = PT0.1S to PT0.1S)"))
    }

    @Test
    fun `should return response with internal server error status`() {
        val returnStatus = ChaosBehaviour.ReturnStatus(NOT_FOUND)
        returnStatus.toString() shouldMatch equalTo(("ReturnStatus (404)"))

        val injectedResponse = returnStatus(tx)
        assertEquals(NOT_FOUND, injectedResponse.status)
    }

    @Test
    fun `should return no body`() {
        val noBody = ChaosBehaviour.NoBody()
        noBody.toString() shouldMatch equalTo(("NoBody"))

        noBody(tx) shouldMatch hasHeader("x-http4k-chaos", "No body").and(hasBody(""))
    }

    @Test
    fun `should block thread`() {
        val blockThread = ChaosBehaviour.BlockThread()
        blockThread.toString() shouldMatch equalTo(("BlockThread"))
        val latch = CountDownLatch(1)
        thread {
            blockThread(tx)
            latch.countDown()
        }

        assertThat(latch.await(100, MILLISECONDS), equalTo(false))
    }

    @Test
    fun `should eat memory`() {
        val eatMemory = ChaosBehaviour.EatMemory()
        eatMemory.toString() shouldMatch equalTo(("EatMemory"))

        assertThat({ eatMemory(tx) }, throws<OutOfMemoryError>())
    }

    @Test
    fun `should do nothing memory`() {
        val none = ChaosBehaviour.None
        none.toString() shouldMatch equalTo(("None"))

        none(tx) shouldMatch equalTo(tx.response)
    }

    @Test
    fun `should provide ability to modify behaviour at runtime`() {
        val variable = Variable()
        variable.toString() shouldMatch equalTo(("Variable [None]"))
        variable(tx) shouldMatch equalTo(tx.response)
        variable.current = NoBody()
        variable.toString() shouldMatch equalTo(("Variable [NoBody]"))
        variable(tx) shouldMatch hasHeader("x-http4k-chaos", "No body").and(hasBody(""))
    }

    @Test
    @Disabled // untestable
    fun `should stack overflow`() {
        val stackOverflow = ChaosBehaviour.StackOverflow()
        stackOverflow.toString() shouldMatch equalTo(("StackOverflow"))
        stackOverflow(tx)
    }

    @Test
    @Disabled // untestable
    fun `should kill process`() {
        val killProcess = ChaosBehaviour.KillProcess()
        killProcess.toString() shouldMatch equalTo(("KillProcess"))
        killProcess(tx)
    }
}
