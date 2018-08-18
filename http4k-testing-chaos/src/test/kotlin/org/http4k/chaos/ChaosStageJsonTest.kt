package org.http4k.chaos

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.chaos.ChaosBehaviours.Latency
import org.http4k.chaos.ChaosBehaviours.NoBody
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosBehaviours.ThrowException
import org.http4k.chaos.ChaosPolicies.Always
import org.http4k.chaos.ChaosPolicies.Once
import org.http4k.chaos.ChaosPolicies.Only
import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.ChaosTriggers.Deadline
import org.http4k.chaos.ChaosTriggers.Delay
import org.http4k.chaos.ChaosTriggers.MatchRequest
import org.http4k.chaos.ChaosTriggers.MatchResponse
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.auto
import org.junit.jupiter.api.Test
import java.time.Clock

data class ChaosStageJson(val type: String?,
                          val policy: String?,
                          val stage: JsonNode?,
                          val behaviour: JsonNode?,
                          val trigger: JsonNode?,
                          val until: JsonNode?) {
    fun toStage(clock: Clock): ChaosStage {
        val baseStage = when (type) {
            "wait" -> Wait
            "repeat" -> Repeat { stage!!.asA<ChaosStageJson>().toStage(clock) }
            "policy" -> {
                when (policy) {
                    "once" -> Once(trigger!!.asTrigger()(clock))
                    "only" -> Only(trigger!!.asTrigger()(clock))
                    "always" -> Always
                    else -> throw IllegalArgumentException("unknown policy")
                }.inject(behaviour!!.asBehaviour())
            }
            else -> throw IllegalArgumentException("unknown stage")
        }
        return until?.let { baseStage.until(until.asTrigger()(clock)) } ?: baseStage
    }

    private fun JsonNode.asTrigger() = when (this["type"]!!.asText()) {
        "deadline" -> asA<Deadline>()
        "delay" -> asA<Delay>()
        "request" -> asA<MatchRequest>()
        "response" -> asA<MatchResponse>()
        else -> throw IllegalArgumentException("unknown trigger")
    }

    private fun JsonNode.asBehaviour() = when (this["type"]!!.asText()) {
        "latency" -> asA<Latency>()
        "throw" -> asA<ThrowException>()
        "status" -> asA<ReturnStatus>()
        "body" -> asA<NoBody>()
        else -> throw IllegalArgumentException("unknown behaviour")
    }
}

class ChaosStageJsonTest {
    val a = """{
    "type": "policy",
    "policy": "once",
    "behaviour": {
      "type": "latency",
      "min": "500",
      "max": "500"
    },
    "until" : {}
  }"""

    @Test
    fun `unmarshalls policy`() {
        val body = Body.auto<ChaosStageJson>().map { it.toStage(Clock.systemUTC()) }

        println(body.toLens()(Request(GET, "").body(a)))
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