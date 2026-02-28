package org.http4k.wiretap.domain

import org.http4k.chaos.Behaviour
import org.http4k.chaos.ChaosBehaviours
import org.http4k.chaos.ChaosTriggers
import org.http4k.chaos.ChaosTriggers.MatchRequest
import org.http4k.chaos.Stage
import org.http4k.chaos.appliedWhen
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import java.time.Duration

data class ChaosConfig(
    val behaviour: String = "ReturnStatus",
    val latencyMin: Int = 100,
    val latencyMax: Int = 500,
    val statusCode: Status = INTERNAL_SERVER_ERROR,
    val trigger: String = "Always",
    val percentage: Int = 50,
    val countdown: Int = 5,
    val delaySeconds: Int = 10,
    val method: Method? = null,
    val path: String? = null,
    val host: String? = null
) {
    fun toBehaviour(): Behaviour = when (behaviour) {
        "Latency" -> ChaosBehaviours.Latency(
            Duration.ofMillis(latencyMin.toLong()),
            Duration.ofMillis(latencyMax.toLong())
        )

        "ReturnStatus" -> ChaosBehaviours.ReturnStatus(statusCode)
        "NoBody" -> ChaosBehaviours.NoBody()
        else -> ChaosBehaviours.ReturnStatus(statusCode)
    }

    fun toTrigger() = when (trigger) {
        "Always" -> ChaosTriggers.Always()
        "PercentageBased" -> ChaosTriggers.PercentageBased(percentage)
        "Once" -> ChaosTriggers.Once()
        "Countdown" -> ChaosTriggers.Countdown(countdown)
        "Delay" -> ChaosTriggers.Delay(Duration.ofSeconds(delaySeconds.toLong()))
        "MatchRequest" -> MatchRequest(
            method = method?.name,
            path = path?.takeIf { it.isNotBlank() }?.let { Regex(".*${Regex.escape(it)}.*", RegexOption.IGNORE_CASE) },
            host = host?.takeIf { it.isNotBlank() }?.let { Regex(".*${Regex.escape(it)}.*", RegexOption.IGNORE_CASE) }
        )
        else -> ChaosTriggers.Always()
    }

    fun toStage(): Stage = toBehaviour().appliedWhen(toTrigger())
}
