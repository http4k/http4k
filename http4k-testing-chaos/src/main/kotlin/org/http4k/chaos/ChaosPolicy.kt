package org.http4k.chaos

import org.http4k.core.HttpTransaction
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

/**
 * Determines whether or not to apply a particular type of ChaosBehaviour to a request/response.
 */
interface ChaosPolicy {
    fun appliesTo(tx: HttpTransaction) = false

    fun inject(behaviour: ChaosBehaviour) = let { it ->
        object : ChaosStage {
            override fun invoke(tx: HttpTransaction) = if (it.appliesTo(tx)) behaviour(tx) else tx.response
            override fun toString() = "$it $behaviour"
        }
    }

    companion object {
        fun Only(trigger: ChaosTrigger) = object : ChaosPolicy {
            override fun appliesTo(tx: HttpTransaction) = trigger(tx)
            override fun toString() = "Only (trigger = $trigger)"
        }

        object Always : ChaosPolicy {
            override fun appliesTo(tx: HttpTransaction) = true
            override fun toString() = "Always"
        }

        object PercentageBased {
            operator fun invoke(injectionFrequency: Int, selector: Random = ThreadLocalRandom.current()) = object : ChaosPolicy {
                override fun appliesTo(tx: HttpTransaction) = selector.nextInt(100) <= injectionFrequency
                override fun toString() = "PercentageBased ($injectionFrequency%)"
            }

            /**
             * Get a percentage from the environment.
             * Defaults to CHAOS_PERCENTAGE and a value of 50%
             */
            fun fromEnvironment(env: (String) -> String? = System::getenv,
                                defaultPercentage: Int = 50,
                                name: String = "CHAOS_PERCENTAGE"
            ) = PercentageBased(env(name)?.let(Integer::parseInt) ?: defaultPercentage)
        }
    }
}