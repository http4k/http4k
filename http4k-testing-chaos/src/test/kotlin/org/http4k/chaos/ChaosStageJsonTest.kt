package org.http4k.chaos

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import org.junit.jupiter.api.Test
import java.time.Clock

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

    @Test
    fun `unmarshalls policy`() {
        val body = Body.auto<List<ChaosStageJson>>().toLens()

        println(body(Request(GET, "").body(a)).flatMap { listOf(it.toStage(Clock.systemUTC())) }
                .reduce { acc, next -> acc.then(next) })
//        val aa = obj(
//                "type" to string("policy"),
//                "policy" to string("once"),
//                "behaviour" to ChaosBehaviours.None.asJsonObject()
//        )
//        Body.json().map {
//            aa["type"]
//            parse(aa)
//        }
//        assertThat(false, equalTo(false))
    }

}