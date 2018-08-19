package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.chaos.ChaosBehaviours.KillProcess
import org.http4k.chaos.ChaosBehaviours.Latency
import org.http4k.chaos.ChaosBehaviours.NoBody
import org.http4k.chaos.ChaosBehaviours.None
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosBehaviours.StackOverflow
import org.http4k.chaos.ChaosBehaviours.ThrowException
import org.http4k.chaos.ChaosBehaviours.Variable
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

private val tx = HttpTransaction(Request(GET, ""), Response(Status.OK).body("hello"), ZERO)

class ThrowExceptionBehaviourTest {
    @Test
    fun `exception throwing behaviour should throw exception`() {
        val expected = RuntimeException("foo")
        val throwException = ThrowException(expected)
        throwException.toString() shouldMatch equalTo(("ThrowException RuntimeException foo"))

        assertThat({ throwException(tx) }, throws(equalTo(expected)))
    }
}

class LatencyBehaviourTest {
    @Test
    @Disabled
    fun `additional latency behaviour should add extra latency`() {
        val delay = 10L
        val latency = Latency(ofMillis(delay), ofMillis(delay + 1))
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

        Latency.fromEnv(props::getProperty).toString() shouldMatch equalTo(("Latency (range = PT1S to PT16M40S)"))
        Latency.fromEnv().toString() shouldMatch equalTo(("Latency (range = PT0.1S to PT0.5S)"))
    }
}

class ReturnStatusBehaviourTest {
    @Test
    fun `should return response with internal server error status`() {
        val returnStatus = ReturnStatus(NOT_FOUND)
        returnStatus.toString() shouldMatch equalTo(("ReturnStatus (404)"))

        val injectedResponse = returnStatus(tx)
        assertEquals(NOT_FOUND, injectedResponse.status)
    }
}

class NoBodyBehaviourTest {
    @Test
    fun `should return no body`() {
        val noBody = NoBody()
        noBody.toString() shouldMatch equalTo(("NoBody"))

        noBody(tx) shouldMatch hasHeader("x-http4k-chaos", "No body").and(hasBody(""))
    }
}

class BlockThreadBehaviourTest {
    @Test
    fun `should block thread`() {
        val blockThread = ChaosBehaviours.BlockThread()
        blockThread.toString() shouldMatch equalTo(("BlockThread"))
        val latch = CountDownLatch(1)
        thread {
            blockThread(tx)
            latch.countDown()
        }

        assertThat(latch.await(100, MILLISECONDS), equalTo(false))
    }
}

class EatMemoryBehaviourTest {
    @Test
    fun `should eat memory`() {
        val eatMemory = ChaosBehaviours.EatMemory()
        eatMemory.toString() shouldMatch equalTo(("EatMemory"))

        assertThat({ eatMemory(tx) }, throws<OutOfMemoryError>())
    }
}

class DoNothingBehaviourTest {
    @Test
    fun `should do nothing memory`() {
        None().toString() shouldMatch equalTo(("None"))

        None()(tx) shouldMatch equalTo(tx.response)
    }
}

class VariableBehaviourTest {
    @Test
    fun `should provide ability to modify behaviour at runtime`() {
        val variable = Variable()
        variable.toString() shouldMatch equalTo(("Variable [None]"))
        variable(tx) shouldMatch equalTo(tx.response)
        variable.current = NoBody()
        variable.toString() shouldMatch equalTo(("Variable [NoBody]"))
        variable(tx) shouldMatch hasHeader("x-http4k-chaos", "No body").and(hasBody(""))
    }
}

class StackOverflowBehaviourTest {
    @Test
    @Disabled // untestable
    fun `should stack overflow`() {
        val stackOverflow = StackOverflow()
        stackOverflow.toString() shouldMatch equalTo(("StackOverflow"))
        stackOverflow(tx)
    }
}

class KillProcessBehaviourTest {

    @Test
    @Disabled // untestable
    fun `should kill process`() {
        val killProcess = KillProcess()
        killProcess.toString() shouldMatch equalTo(("KillProcess"))
        killProcess(tx)
    }
}
