package org.http4k.chaos

import org.http4k.core.Filter
import org.http4k.core.HttpTransaction
import org.http4k.core.Response
import java.time.Clock
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Defines a period during which a particular ChaosBehaviour to be active.
 */
interface ChaosStage: ChaosBehaviour {
    fun done(): Boolean = false

    companion object {
        fun Repeat(stage: () -> ChaosStage): ChaosStage = object : ChaosStage {
            private val current by lazy { AtomicReference(stage()) }
            override fun done(): Boolean {
                if (current.get().done())
                    current.set(stage())
                return current.get().done()
            }

            override fun invoke(tx: HttpTransaction) = current.get()(tx)
        }

        object Wait : ChaosStage
    }
}

fun ChaosStage.asFilter(clock: Clock = Clock.systemUTC()) = Filter { next ->
    {
        clock.instant().let { start ->
            next(it).apply { this@asFilter(HttpTransaction(it, this, Duration.between(start, clock.instant()))) }
        }
    }
}

fun ChaosStage.then(next: ChaosStage): ChaosStage = object : ChaosStage {
    override fun done() = this@then.done().let { if (it) next.done() else it }
    override fun invoke(tx: HttpTransaction): Response = if (done()) next(tx) else this@then(tx)
}

fun ChaosStage.until(trigger: TransactionTrigger) = object : ChaosStage {
    private val isDone = AtomicBoolean(false)
    override fun done(): Boolean = isDone.get()
    override fun invoke(tx: HttpTransaction): Response {
        if (!done()) isDone.set(trigger(tx))
        return this@until(tx)
    }
}
