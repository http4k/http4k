package org.http4k.chaos

import org.http4k.core.Filter
import org.http4k.core.HttpTransaction
import org.http4k.core.Response
import java.time.Clock
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Defines a periodic element during which a particular ChaosBehaviour is active.
 */
interface ChaosStage {
    operator fun invoke(tx: HttpTransaction): Response?

    companion object {
        /**
         * Repeats a stage (or composite stage in repeating pattern). Since ChaosStages are STATEFUL,
         * the stage function will be fired on each iteration and expecting a NEW instance.
         */
        fun Repeat(newStageFn: () -> ChaosStage): ChaosStage = object : ChaosStage {
            private val current by lazy { AtomicReference(newStageFn()) }

            override fun invoke(tx: HttpTransaction): Response? =
                    current.get()(tx) ?: run {
                        current.set(newStageFn())
                        current.get()(tx)
                    }
        }

        /**
         * Does not apply any ChaosBehaviour.
         */
        object Wait : ChaosStage {
            override fun invoke(tx: HttpTransaction): Response? = null
        }
    }

    /**
     * Chain the next ChaosBehaviour to apply when this stage is finished.
     */
    fun then(nextStage: ChaosStage): ChaosStage = let {
        object : ChaosStage {
            override fun invoke(tx: HttpTransaction): Response? = it(tx) ?: nextStage(tx)
        }
    }

    /**
     * Stop applying the ChaosBehaviour of this stage when the StageTrigger fires.
     */
    fun until(stageTrigger: ChaosStageTrigger): ChaosStage {
        val first = this
        val active = AtomicBoolean(true)

        return object : ChaosStage {
            override fun invoke(tx: HttpTransaction): Response? {
                if (active.get()) active.set(!stageTrigger(tx))
                return if (active.get()) first(tx) else null
            }
        }
    }

    /**
     * Converts this chaos behaviour to a standard http4k Filter.
     */
    fun asFilter(clock: Clock = Clock.systemUTC()): Filter {
        val first = this
        return Filter { next ->
            {
                clock.instant().let { start ->
                    next(it).run {
                        first(HttpTransaction(it, this, Duration.between(start, clock.instant()))) ?: this
                    }
                }
            }
        }
    }
}