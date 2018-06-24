package org.http4k.chaos

import org.http4k.chaos.ChaosPeriod.Companion.BlockThread
import org.http4k.chaos.ChaosPeriod.Companion.Latency
import org.http4k.chaos.ChaosPeriod.Companion.Repeat
import org.http4k.chaos.ChaosPeriod.Companion.Wait
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.lens.Header
import java.time.Duration

val Header.Common.CHAOS; get() = Header.required("x-http4k-chaos")

object ChaosFilters {
    operator fun invoke(chaosPolicy: ChaosPolicy, behaviour: ChaosBehaviour) = Filter { next ->
        {
            if (chaosPolicy.shouldInject(it)) {
                next(behaviour.inject(it)).with(Header.Common.CHAOS of behaviour.description)
            } else next(it).let {
                if (chaosPolicy.shouldInject(it)) {
                    behaviour.inject(it).with(Header.Common.CHAOS of behaviour.description)
                } else it
            }
        }
    }
}

interface ChaosPeriod {
    companion object {
        fun Repeat(period: ChaosPeriod): ChaosPeriod = TODO()
        fun Repeat(times: Int, period: ChaosPeriod): ChaosPeriod = TODO()

        object Wait: ChaosPeriod

        fun BlockThread(): ChaosPeriod = TODO()
        fun Latency(): ChaosPeriod = TODO()
        // eat memory
        // stack overflow
    }
}

fun ChaosPeriod.then(next: ChaosPeriod): ChaosPeriod = TODO()

@JvmName("untilResponse")
fun ChaosPeriod.until(trigger: (Response) -> Boolean): ChaosPeriod = TODO()
fun ChaosPeriod.until(trigger: (Request) -> Boolean): ChaosPeriod = TODO()
fun ChaosPeriod.until(trigger: () -> Boolean): ChaosPeriod = TODO()
fun ChaosPeriod.until(period: Duration): ChaosPeriod = TODO()

val blockThread = Wait.until(Duration.ofSeconds(100)).then(BlockThread())
val goSlow = Wait.until(Duration.ofSeconds(100)).then(Latency())
val a = Repeat(blockThread.then(goSlow)).until { _: Response -> true}
