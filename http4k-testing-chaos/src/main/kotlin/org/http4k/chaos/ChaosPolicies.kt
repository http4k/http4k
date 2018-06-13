package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.util.concurrent.ThreadLocalRandom

interface ChaosPolicy {
    fun shouldInject(request: Request): Boolean = false
    fun shouldInject(response: Response): Boolean = false

    companion object {
        fun PercentageBased(injectionFrequency: Int) = object : ChaosPolicy {
            override fun shouldInject(response: Response): Boolean {
                return ThreadLocalRandom.current().nextInt(0, 100) < injectionFrequency
            }
        }

        fun PercentageBasedFromEnv(): ChaosPolicy {
            val injectionFrequency = (System.getenv("CHAOS_INJECTION_FREQUENCY") ?: "50").toInt()
            return PercentageBased(injectionFrequency)
        }
    }
}
