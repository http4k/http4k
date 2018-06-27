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
        fun Repeat(stage: () -> ChaosStage): ChaosStage = object : ChaosStage {
            private val current by lazy { AtomicReference(stage()) }

            override fun invoke(tx: HttpTransaction) = current.get()(tx) ?: run {
                current.set(stage())
                current.get()(tx)
            }
        }

        /**
         * Does not apply any behaviour to
         */
        object Wait : ChaosStage {
            override fun invoke(tx: HttpTransaction): Response? = null
        }
    }

    fun then(next: ChaosStage): ChaosStage = let {
        object : ChaosStage {
            override fun invoke(tx: HttpTransaction): Response? = it(tx) ?: next(tx)
        }
    }

    fun until(trigger: TransactionTrigger): ChaosStage {
        val first = this
        val active = AtomicBoolean(true)

        return object : ChaosStage {
            override fun invoke(tx: HttpTransaction): Response? {
                if (active.get()) active.set(!trigger(tx))
                return if (active.get()) first(tx) else null
            }
        }
    }

    /**
     * Converts the
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