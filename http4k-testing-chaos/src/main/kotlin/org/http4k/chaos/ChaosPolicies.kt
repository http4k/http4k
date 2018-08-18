package org.http4k.chaos

import org.http4k.core.HttpTransaction
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Determines whether or not to apply a particular type of ChaosBehaviour to a request/response.
 */
typealias ChaosPolicy = (HttpTransaction) -> Boolean

/**
 * Returns a ChaosStage which applies some ChaosBehaviour based upon if the policy applies to the
 * passed transaction.
 */
fun ChaosPolicy.inject(behaviour: ChaosBehaviour) = let { it ->
    object : ChaosStage {
        override fun invoke(tx: HttpTransaction) = if (it(tx)) behaviour(tx) else tx.response
        override fun toString() = "$it $behaviour"
    }
}

object ChaosPolicies {
    /**
     * Single application predicated on the ChaosTrigger. Further matches don't apply
     */
    object Once {
        operator fun invoke(trigger: ChaosTrigger) = object : ChaosPolicy {
            private val active = AtomicBoolean(true)
            override fun invoke(tx: HttpTransaction) =
                    if (trigger(tx)) active.get().also { active.set(false) } else false

            override fun toString() = "Once (trigger = $trigger)"
        }
    }

    /**
     * Application predicated on the ChaosTrigger
     */
    object Only {
        operator fun invoke(trigger: ChaosTrigger) = object : ChaosPolicy {
            override fun invoke(tx: HttpTransaction) = trigger(tx)
            override fun toString() = "Only (trigger = $trigger)"
        }
    }

    /**
     * Applies to every transaction.
     */
    object Always : ChaosPolicy {
        override fun invoke(tx: HttpTransaction) = true
        override fun toString() = "Always"
    }

    /**
     * Applies n% of the time, based on result of a Random.
     */
    object PercentageBased {
        operator fun invoke(injectionFrequency: Int, selector: Random = ThreadLocalRandom.current()) = object : ChaosPolicy {
            override fun invoke(tx: HttpTransaction) = selector.nextInt(100) <= injectionFrequency
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