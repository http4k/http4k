package org.http4k.chaos

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.asJsonObject
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.time.Duration

private val tx = HttpTransaction(Request(GET, ""), Response(OK).body("hello"), Duration.ZERO)


class ChaosStageJsonTest {
    val a = """[{
    "type": "policy",
    "policy": "once",
    "behaviour": {
      "type": "latency",
      "min": "500",
      "max": "500"
    },
    "until" : {}
  }]"""

    /**
    [
    {
    "type": "policy",
    "policy": "once",
    "behaviour": {
    "type": "latency",
    "min": "500",
    "max": "500"
    },
    "until": {}
    },
    {
    "type": "policy",
    "policy": "percentage",
    "percentage": 11,
    "behaviour": {
    "type": "latency",
    "min": "500",
    "max": "500"
    },
    "until": {}
    },
    {
    "type": "repeat",
    "stage": {
    "type": "policy",
    "policy": "once",
    "trigger": {
    "type": "latency",
    "min": "500",
    "max": "500"
    },
    "until": {}
    },
    "until": {}
    }
    ]
     */

    @Test
    fun `unmarshall no body`() {
        assertBehaviour("""{"type":"body"}""",
                "NoBody",
                hasStatus(OK).and(hasHeader("x-http4k-chaos", "No body")))
    }

    @Test
    fun `unmarshall latency`() {
        assertBehaviour("""{"type":"latency","min":"PT0.01S","max":"PT0.03S"}""",
                "Latency (range = PT0.01S to PT0.03S)",
                hasStatus(OK).and(hasHeader("x-http4k-chaos", Regex("Latency.*"))))
    }

    @Test
    fun `unmarshall status`() {
        assertBehaviour("""{"type":"status","status":404}""",
                "ReturnStatus (404)",
                hasStatus(NOT_FOUND.description("x-http4k-chaos")).and(hasHeader("x-http4k-chaos", Regex("Status 404"))))
    }

    @Test
    fun `unmarshall throw`() {
        val behaviour = """{"message":"boo","type":"throw"}""".asJsonObject().asBehaviour()
        behaviour.toString() shouldMatch equalTo(("ThrowException Exception boo"))

        assertThat({ behaviour(tx) }, throws<Exception>())
    }

    private fun assertBehaviour(json: String, description: String, matcher: Matcher<Response>) {
        val behaviour = json.asJsonObject().asBehaviour()
        behaviour.toString() shouldMatch equalTo(description)
        behaviour(tx) shouldMatch matcher
    }

//    @Test
//    fun `unmarshalls policy`() {
//        val body = Body.auto<List<ChaosStageJson>>().toLens()
//
//        println(body(Request(GET, "").body(a)).flatMap { listOf(it.toStage(Clock.systemUTC())) }
//                .reduce { acc, next -> acc.then(next) })
////        val aa = obj(
////                "type" to string("policy"),
////                "policy" to string("once"),
////                "behaviour" to ChaosBehaviours.None.asJsonObject()
////        )
////        Body.json().map {
////            aa["type"]
////            parse(aa)
////        }
////        assertThat(false, equalTo(false))
//    }

}