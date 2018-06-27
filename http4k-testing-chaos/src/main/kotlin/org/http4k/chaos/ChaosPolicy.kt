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
        }
    }

    companion object {
        fun Only(trigger: StageTrigger) = object : ChaosPolicy {
            override fun appliesTo(tx: HttpTransaction) = trigger(tx)
        }

        object Always : ChaosPolicy {
            override fun appliesTo(tx: HttpTransaction) = true
        }

        fun PercentageBased(injectionFrequency: Int, selector: Random = ThreadLocalRandom.current()) = object : ChaosPolicy {
            override fun appliesTo(tx: HttpTransaction) = selector.nextInt(100) <= injectionFrequency
        }
    }
}