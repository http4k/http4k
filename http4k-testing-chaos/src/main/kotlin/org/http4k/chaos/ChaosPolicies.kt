package org.http4k.chaos

import org.http4k.core.HttpMessage
import java.util.concurrent.ThreadLocalRandom

interface ChaosPolicy {
    fun shouldInject(httpMessage: HttpMessage): Boolean
}

open class PercentageBasedChaosPolicy(private val injectionFrequency: Int) : ChaosPolicy {
    override fun shouldInject(httpMessage: HttpMessage): Boolean {
        return ThreadLocalRandom.current().nextInt(0, 100) < injectionFrequency
    }

    companion object {
        fun fromEnv(): PercentageBasedChaosPolicy {
            val injectionFrequency = (System.getenv("CHAOS_INJECTION_FREQUENCY") ?: "50").toInt()
            return PercentageBasedChaosPolicy(injectionFrequency)
        }
    }
}

class AlwaysInjectChaosPolicy : ChaosPolicy {
    override fun shouldInject(httpMessage: HttpMessage) = true
}
