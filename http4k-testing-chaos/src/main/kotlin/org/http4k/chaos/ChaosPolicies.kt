package org.http4k.chaos

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.chaos.ChaosPolicies.Always
import org.http4k.chaos.ChaosPolicies.Once
import org.http4k.chaos.ChaosPolicies.Only
import org.http4k.chaos.ChaosPolicies.PercentageBased
import org.http4k.core.Request
import java.time.Clock
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Determines whether or not to apply a particular type of ChaosBehaviour to a request.
 */
typealias Policy = (Request) -> Boolean

/**
 * Returns a ChaosStage which applies some ChaosBehaviour based upon if the policy applies to the
 * passed transaction.
 */
fun Policy.inject(behaviour: Behaviour) = let { it ->
    object : Stage {
        override fun invoke(tx: Request) = if (it(tx)) behaviour else null
        override fun toString() = "$it $behaviour"
    }
}

internal fun JsonNode.asPolicy(clock: Clock = Clock.systemUTC()) = when (nonNullable<String>("type")) {
    "once" -> Once(this["trigger"].asTrigger(clock))
    "only" -> Only(this["trigger"].asTrigger(clock))
    "percentage" -> PercentageBased(this["percentage"].asInt())
    "always" -> Always
    else -> throw IllegalArgumentException("unknown policy")
}

object ChaosPolicies {
    /**
     * Single application predicated on the ChaosTrigger. Further matches don't apply
     */
    object Once {
        operator fun invoke(trigger: Trigger) = object : Policy {
            private val active = AtomicBoolean(true)
            override fun invoke(request: Request) =
                    if (trigger(request)) active.get().also { active.set(false) } else false

            override fun toString() = "Once (trigger = $trigger)"
        }
    }

    /**
     * Application predicated on the ChaosTrigger
     */
    object Only {
        operator fun invoke(trigger: Trigger) = object : Policy {
            override fun invoke(request: Request) = trigger(request)
            override fun toString() = "Only (trigger = $trigger)"
        }
    }

    /**
     * Applies to every transaction.
     */
    object Always : Policy {
        override fun invoke(request: Request) = true
        override fun toString() = "Always"
    }

    /**
     * Applies n% of the time, based on result of a Random.
     */
    object PercentageBased {
        operator fun invoke(injectionFrequency: Int, selector: Random = ThreadLocalRandom.current()) = object : Policy {
            override fun invoke(request: Request) = selector.nextInt(100) <= injectionFrequency
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