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
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.Jackson.asJsonObject
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
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

abstract class ChaosBehaviourContract {
    @Test
    abstract fun `deserialises from JSON`()
}

class ThrowExceptionBehaviourTest : ChaosBehaviourContract() {
    val description = "ThrowException RuntimeException foo"

    @Test
    fun `exception throwing behaviour should throw exception`() {
        val expected = RuntimeException("foo")
        val throwException = ThrowException(expected)
        throwException.toString() shouldMatch equalTo(description)

        assertThat({ throwException.then { tx.response }(tx.request) }, throws(equalTo(expected)))
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"message":"foo","type":"throw"}""".asJsonObject().asBehaviour()
        behaviour.toString() shouldMatch equalTo("ThrowException RuntimeException foo")

        assertThat({ behaviour.then { tx.response }(tx.request) }, throws<Exception>())
    }
}

class LatencyBehaviourTest : ChaosBehaviourContract() {
    val description = "Latency (range = PT0.1S to PT0.3S)"

    @Test
    fun `latency from env`() {
        val props = Properties().apply {
            put("CHAOS_LATENCY_MS_MIN", "100")
            put("CHAOS_LATENCY_MS_MAX", "300")
        }

        Latency.fromEnv(props::getProperty).toString() shouldMatch equalTo(description)
        Latency.fromEnv().toString() shouldMatch equalTo("Latency (range = PT0.1S to PT0.5S)")
    }

    @Test
    override fun `deserialises from JSON`() {
        assertBehaviour("""{"type":"latency","min":"PT0.1S","max":"PT0.3S"}""",
                description,
                hasStatus(Status.OK).and(hasHeader("x-http4k-chaos", Regex("Latency.*"))))
    }

    @Test
    fun `latency behaviour should add extra latency`() {
        val delay = 100L
        val latency = Latency(ofMillis(delay), ofMillis(delay + 1))
        latency.toString() shouldMatch equalTo("Latency (range = PT0.1S to PT0.101S)")

        val latch = CountDownLatch(1)
        thread {
            latency.then { tx.response }(tx.request)
            latch.countDown()
        }
        assertThat(latch.await(delay - 1, MILLISECONDS), equalTo(false))
    }
}

class ReturnStatusBehaviourTest : ChaosBehaviourContract() {
    val description = "ReturnStatus (404)"

    @Test
    fun `should return response with internal server error status`() {
        val returnStatus: Behaviour = ReturnStatus(NOT_FOUND)
        returnStatus.toString() shouldMatch equalTo(description)

        val injectedResponse = returnStatus.then { tx.response }(tx.request)
        assertEquals(NOT_FOUND, injectedResponse.status)
    }

    @Test
    override fun `deserialises from JSON`() {
        assertBehaviour("""{"type":"status","status":404}""",
                description,
                hasStatus(NOT_FOUND.description("x-http4k-chaos")).and(hasHeader("x-http4k-chaos", Regex("Status 404"))))
    }
}

class NoBodyBehaviourTest : ChaosBehaviourContract() {
    val description = "NoBody"

    @Test
    fun `should return no body`() {
        val noBody = NoBody()
        noBody.toString() shouldMatch equalTo(description)

        noBody.then { tx.response }(tx.request) shouldMatch hasHeader("x-http4k-chaos", "No body").and(hasBody(""))
    }

    @Test
    override fun `deserialises from JSON`() {
        assertBehaviour("""{"type":"body"}""",
                description,
                hasStatus(OK).and(hasHeader("x-http4k-chaos", "No body")))
    }
}

class BlockThreadBehaviourTest : ChaosBehaviourContract() {
    val description = "BlockThread"

    @Test
    fun `should block thread`() {
        val blockThread = ChaosBehaviours.BlockThread()
        blockThread.toString() shouldMatch equalTo(description)
        val latch = CountDownLatch(1)
        thread {
            blockThread.then { tx.response }(tx.request)
            latch.countDown()
        }

        assertThat(latch.await(100, MILLISECONDS), equalTo(false))
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"block"}""".asJsonObject().asBehaviour()
        behaviour.toString() shouldMatch equalTo(description)
    }
}

class EatMemoryBehaviourTest : ChaosBehaviourContract() {
    val description = "EatMemory"

    @Test
    fun `should eat memory`() {
        val eatMemory = ChaosBehaviours.EatMemory()
        eatMemory.toString() shouldMatch equalTo(description)

        assertThat({ eatMemory.then { tx.response }(tx.request) }, throws<OutOfMemoryError>())
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"memory"}""".asJsonObject().asBehaviour()
        behaviour.toString() shouldMatch equalTo(description)
    }
}

class DoNothingBehaviourTest : ChaosBehaviourContract() {
    private val description = "None"

    @Test
    fun `should do nothing memory`() {
        None().toString() shouldMatch equalTo(description)

        None().then { tx.response }(tx.request) shouldMatch equalTo(tx.response)
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"none"}""".asJsonObject().asBehaviour()
        behaviour.toString() shouldMatch equalTo(description)
    }
}

class StackOverflowBehaviourTest : ChaosBehaviourContract() {
    private val description = "StackOverflow"

    @Test
    @Disabled // untestable
    fun `should stack overflow`() {
        val stackOverflow = StackOverflow()
        stackOverflow.toString() shouldMatch equalTo(description)
        stackOverflow.then { tx.response }(tx.request)
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"overflow"}""".asJsonObject().asBehaviour()
        behaviour.toString() shouldMatch equalTo(description)
    }
}

class KillProcessBehaviourTest : ChaosBehaviourContract() {

    private val description = "KillProcess"

    @Test
    @Disabled // untestable
    fun `should kill process`() {
        val killProcess = KillProcess()
        killProcess.toString() shouldMatch equalTo(description)
        killProcess.then { tx.response }(tx.request)
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"kill"}""".asJsonObject().asBehaviour()
        behaviour.toString() shouldMatch equalTo(description)
    }
}

class VariableBehaviourTest {
    @Test
    fun `should provide ability to modify behaviour at runtime`() {
        val variable = Variable()
        variable.toString() shouldMatch equalTo(("Variable [None]"))
        variable.then { tx.response }(tx.request) shouldMatch equalTo(tx.response)
        variable.current = NoBody()
        variable.toString() shouldMatch equalTo(("Variable [NoBody]"))
        variable.then { tx.response }(tx.request) shouldMatch hasHeader("x-http4k-chaos", "No body").and(hasBody(""))
    }
}