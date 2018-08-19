package org.http4k.chaos

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.asJsonObject
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