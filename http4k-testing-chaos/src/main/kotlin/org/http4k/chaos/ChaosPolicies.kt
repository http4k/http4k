package org.http4k.chaos

import org.http4k.core.HttpTransaction
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Determines whether or not to apply a particular type of ChaosBehaviour to a request/response.
 */
interface ChaosPolicy {
    fun appliesTo(tx: HttpTransaction) = false

    /**
     * Returns a ChaosStage which applies some ChaosBehaviour based upon if the policy applies to the
     * passed transaction.
     */
    fun inject(behaviour: ChaosBehaviour) = let { it ->
        object : ChaosStage {
            override fun invoke(tx: HttpTransaction) = if (it.appliesTo(tx)) behaviour(tx) else tx.response
            override fun toString() = "$it $behaviour"
        }
    }
}

object ChaosPolicies {
    /**
     * Single application predicated on the ChaosTrigger. Further matches don't apply
     */
    data class Once(val trigger: ChaosTrigger) : ChaosPolicy {
        private val active = AtomicBoolean(true)
        override fun appliesTo(tx: HttpTransaction) =
                if (trigger(tx)) active.get().also { active.set(false) } else false

        override fun toString() = "Once (trigger = $trigger)"
    }

    /**
     * Application predicated on the ChaosTrigger
     */
    data class Only(val trigger: ChaosTrigger) : ChaosPolicy {
        override fun appliesTo(tx: HttpTransaction) = trigger(tx)
        override fun toString() = "Only (trigger = $trigger)"
    }

    /**
     * Applies to every transaction.
     */
    object Always : ChaosPolicy {
        override fun appliesTo(tx: HttpTransaction) = true
        override fun toString() = "Always"
    }

    /**
     * Applies n% of the time, based on result of a Random.
     */
    data class PercentageBased(val injectionFrequency: Int, val selector: Random = ThreadLocalRandom.current()) : ChaosPolicy {
        override fun appliesTo(tx: HttpTransaction) = selector.nextInt(100) <= injectionFrequency
        override fun toString() = "PercentageBased ($injectionFrequency%)"

        companion object {
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