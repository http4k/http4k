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
typealias ChaosStage = (HttpTransaction) -> Response?

/**
 * Chain the next ChaosBehaviour to apply when this stage is finished.
 */
fun ChaosStage.then(nextStage: ChaosStage): ChaosStage = let {
    object : ChaosStage {
        override fun invoke(tx: HttpTransaction): Response? = it(tx) ?: nextStage(tx)
        override fun toString() = "[$it] then [$nextStage]"
    }
}

/**
 * Stop applying the ChaosBehaviour of this stage when the ChaosTrigger fires.
 */
fun ChaosStage.until(trigger: ChaosTrigger): ChaosStage = let {
    object : ChaosStage {
        private val active = AtomicBoolean(true)
        override fun invoke(tx: HttpTransaction): Response? {
            if (active.get()) active.set(!trigger(tx))
            return if (active.get()) it(tx) else null
        }

        override fun toString(): String = "$it until $trigger"
    }
}

/**
 * Converts this chaos behaviour to a standard http4k Filter.
 */
fun ChaosStage.asFilter(clock: Clock = Clock.systemUTC()): Filter = let {
    Filter { next ->
        { req ->
            clock.instant().let { start ->
                next(req).run {
                    it(HttpTransaction(req, this, Duration.between(start, clock.instant()))) ?: this
                }
            }
        }
    }
}

object ChaosStages {
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

        override fun toString() = "Repeat ${current.get()}"
    }

    /**
     * Does not apply any ChaosBehaviour.
     */
    object Wait : ChaosStage {
        override fun invoke(tx: HttpTransaction) = tx.response
        override fun toString() = "Wait"
    }
}