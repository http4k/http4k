package org.http4k.wiretap.domain

import org.http4k.chaos.Behaviour
import org.http4k.chaos.ChaosBehaviours
import org.http4k.chaos.ChaosTriggers
import org.http4k.chaos.Stage
import org.http4k.chaos.Trigger
import org.http4k.chaos.and
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
        else -> ChaosTriggers.Always()
    }

    fun toFilterTrigger(): Trigger {
        val matchers = listOfNotNull(
            method?.let { m -> Trigger { it.method == m } },
            path?.takeIf { it.isNotBlank() }?.let { p ->
                Trigger { it.uri.path.contains(p, ignoreCase = true) }
            },
            host?.takeIf { it.isNotBlank() }?.let { h ->
                Trigger { req ->
                    val requestHost = req.uri.host.takeIf { it.isNotEmpty() } ?: req.header("Host") ?: ""
                    requestHost.contains(h, ignoreCase = true)
                }
            }
        )
        return matchers.reduceOrNull { acc, next -> acc and next } ?: ChaosTriggers.Always()
    }

    fun toStage(): Stage {
        val hasFilters = listOfNotNull(
            method,
            path?.takeIf { it.isNotBlank() },
            host?.takeIf { it.isNotBlank() }
        ).isNotEmpty()
        val trigger = if (hasFilters) toFilterTrigger() and toTrigger() else toTrigger()
        return toBehaviour().appliedWhen(trigger)
    }
}
