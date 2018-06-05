package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.util.concurrent.ThreadLocalRandom

interface ChaosPolicy {
    fun shouldInject(request: Request): Boolean = false
    fun shouldInject(response: Response): Boolean = false
}

open class PercentageBasedChaosPolicy(private val injectionFrequency: Int) : ChaosPolicy {
    override fun shouldInject(response: Response): Boolean {
        return ThreadLocalRandom.current().nextInt(0, 100) < injectionFrequency
    }

    companion object {
        fun fromEnv(): PercentageBasedChaosPolicy {
            val injectionFrequency = (System.getenv("CHAOS_INJECTION_FREQUENCY") ?: "50").toInt()
            return PercentageBasedChaosPolicy(injectionFrequency)
        }
    }
}
