package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.util.concurrent.ThreadLocalRandom

interface ChaosPolicy {
    fun shouldInject(request: Request): Boolean = false
    fun shouldInject(response: Response): Boolean = false

    companion object {
        fun Always(injectRequest: Boolean = true, injectResponse: Boolean = true) = object : ChaosPolicy {
            override fun shouldInject(request: Request): Boolean = injectRequest
            override fun shouldInject(response: Response): Boolean = injectResponse
        }

        fun PercentageBased(injectionFrequency: Int) = object : ChaosPolicy {
            override fun shouldInject(response: Response): Boolean =
                    ThreadLocalRandom.current().nextInt(0, 100) < injectionFrequency
        }

        fun PercentageBasedFromEnv(): ChaosPolicy {
            val injectionFrequency = (System.getenv("CHAOS_INJECTION_FREQUENCY") ?: "50").toInt()
            return PercentageBased(injectionFrequency)
        }
    }
}
