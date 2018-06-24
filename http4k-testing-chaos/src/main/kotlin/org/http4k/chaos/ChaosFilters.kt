package org.http4k.chaos

import org.http4k.chaos.ChaosPeriod.Companion.BlockThread
import org.http4k.chaos.ChaosPeriod.Companion.Latency
import org.http4k.chaos.ChaosPeriod.Companion.Repeat
import org.http4k.chaos.ChaosPeriod.Companion.Wait
import org.http4k.chaos.ChaosPolicy.Companion.PercentageBased
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.lens.Header
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

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

    operator fun invoke(chaosPeriod: ChaosPeriod) = Filter { next ->
        {
            chaosPeriod(next(chaosPeriod(it)))
        }
    }
}

interface ChaosPeriod {
    operator fun invoke(request: Request) = request
    operator fun invoke(response: Response) = response

    companion object {
        fun Repeat(period: ChaosPeriod): ChaosPeriod = TODO()

        object Wait : ChaosPeriod

        fun BlockThread() = object : ChaosPeriod {
            override fun invoke(request: Request) = request.apply { Thread.currentThread().join() }
        }

        fun Latency(minDelay: Duration = Duration.ofMillis(100),
                    maxDelay: Duration = Duration.ofMillis(500)) = object : ChaosPeriod {
            override fun invoke(response: Response): Response = response.apply {
                val delay = ThreadLocalRandom.current().nextInt(minDelay.toMillis().toInt(), maxDelay.toMillis().toInt())
                Thread.sleep(delay.toLong())
            }
        }

        @Suppress("unused")
        fun EatMemory(): ChaosPeriod = object : ChaosPeriod {
            override fun invoke(response: Response): Response = response.apply {
                val list = mutableListOf<ByteArray>()
                while (true) list += ByteArray(1048576)
            }
        }

        @Suppress("unused")
        fun KillProcess(): ChaosPeriod = object : ChaosPeriod {
            override fun invoke(request: Request) = request.apply { System.exit(1) }
        }

        @Suppress("unused")
        fun StackOverflow(): ChaosPeriod = object : ChaosPeriod {
            override fun invoke(request: Request) = request.apply { StackOverflow() }
        }

        fun ThrowException(e: Exception = ChaosException("Chaos behaviour injected!")) = object : ChaosPeriod {
            override fun invoke(request: Request) = throw e
        }
    }
}

fun ChaosPeriod.then(next: ChaosPeriod): ChaosPeriod = TODO()

@JvmName("untilResponse")
fun ChaosPeriod.until(trigger: (Response) -> Boolean): ChaosPeriod = TODO()

fun ChaosPeriod.until(trigger: (Request) -> Boolean): ChaosPeriod = TODO()
fun ChaosPeriod.until(trigger: () -> Boolean): ChaosPeriod = TODO()
fun ChaosPeriod.until(period: Duration): ChaosPeriod = TODO()

val blockThread = Wait.until(Duration.ofSeconds(100)).then(PercentageBased(100)(BlockThread()))
val goSlow = Wait.until(Duration.ofSeconds(100)).then(Latency(Duration.ofMillis(1)))
val a = Repeat(blockThread.then(goSlow)).until { _: Response -> true }
