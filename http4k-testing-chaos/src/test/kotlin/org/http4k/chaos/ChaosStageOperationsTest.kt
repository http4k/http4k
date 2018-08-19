package org.http4k.chaos

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Method.TRACE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.Jackson.asJsonObject
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

private val response = Response(OK).body("body")

abstract class ChaosStageContract {
    abstract val asJson: String
    abstract val expectedDescription: String

    @Test
    fun `deserialises from JSON`() {
        val clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"))
        asJson.asJsonObject().asStage(clock).toString() shouldMatch equalTo(expectedDescription)
    }
}

class WaitTest : ChaosStageContract() {
    override val asJson = """{"type":"wait"}"""
    override val expectedDescription = "Wait"

    @Test
    fun `Wait does not match the response`() {
        val app = Wait.asFilter().then { response }
        app(Request(GET, "")) shouldMatch equalTo(response)
    }
}

class RepeatTest : ChaosStageContract() {
    override val asJson = """{"type":"repeat","stage":{"type":"wait"}}"""
    override val expectedDescription = "Repeat Wait"

    @Test
    fun `repeat starts again at the beginning`() {
        val app = Repeat {
            chaosStage(I_M_A_TEAPOT).until { it.request.method == POST }
                    .then(chaosStage(NOT_FOUND).until { it.request.method == OPTIONS })
                    .then(chaosStage(GATEWAY_TIMEOUT).until { it.request.method == TRACE })
        }.until { it.request.method == DELETE }
                .asFilter().then { response }

        app(Request(GET, "")) shouldMatch equalTo(Response(I_M_A_TEAPOT))
        app(Request(POST, "")) shouldMatch equalTo(Response(NOT_FOUND))
        app(Request(GET, "")) shouldMatch equalTo(Response(NOT_FOUND))
        app(Request(OPTIONS, "")) shouldMatch equalTo(Response(GATEWAY_TIMEOUT))
        app(Request(GET, "")) shouldMatch equalTo(Response(GATEWAY_TIMEOUT))
        app(Request(TRACE, "")) shouldMatch equalTo(Response(I_M_A_TEAPOT))
        app(Request(DELETE, "")) shouldMatch equalTo(response)
    }
}

class ChaosStageOperationsTest {
    @Test
    fun `until stops when the trigger is hit`() {
        val app = chaosStage(NOT_FOUND).until { it.request.method == POST }
                .asFilter().then { response }

        app(Request(GET, "")) shouldMatch equalTo(Response(NOT_FOUND))
        app(Request(POST, "")) shouldMatch equalTo(response)
        app(Request(GET, "")) shouldMatch equalTo(response)
    }

    @Test
    fun `then moves onto the next stage`() {
        val app = chaosStage(I_M_A_TEAPOT).until { it.request.method == POST }
                .then(chaosStage(NOT_FOUND).until { it.request.method == TRACE })
                .then(chaosStage(INTERNAL_SERVER_ERROR))
                .asFilter().then { response }

        app(Request(GET, "")) shouldMatch equalTo(Response(I_M_A_TEAPOT))
        app(Request(POST, "")) shouldMatch equalTo(Response(NOT_FOUND))
        app(Request(GET, "")) shouldMatch equalTo(Response(NOT_FOUND))
        app(Request(TRACE, "")) shouldMatch equalTo(Response(INTERNAL_SERVER_ERROR))
        app(Request(GET, "")) shouldMatch equalTo(Response(INTERNAL_SERVER_ERROR))
    }
}

private fun chaosStage(status: Status): ChaosStage = object : ChaosStage {
    override fun invoke(tx: HttpTransaction) = Response(status)
}