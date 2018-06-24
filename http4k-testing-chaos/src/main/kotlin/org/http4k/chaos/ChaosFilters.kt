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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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
    fun done(): Boolean = false
    operator fun invoke(request: Request) = request
    operator fun invoke(response: Response) = response

    companion object {
        fun Repeat(newPeriod: () -> ChaosPeriod): ChaosPeriod = object : ChaosPeriod {
            private val current by lazy { AtomicReference(newPeriod()) }
            override fun done(): Boolean {
                if (current.get().done())
                    current.set(newPeriod())
                return current.get().done()
            }

            override fun invoke(request: Request) = current.get()(request)
            override fun invoke(response: Response) = current.get()(response)
        }

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
            override fun invoke(response: Response) = response.apply {
                mutableListOf<ByteArray>().let { while (true) it += ByteArray(1024 * 1024) }
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

fun ChaosPeriod.then(next: ChaosPeriod): ChaosPeriod = object : ChaosPeriod {
    override fun done() = this@then.done().let { if (it) next.done() else it }
    override fun invoke(request: Request): Request = if (done()) next(request) else this@then(request)
    override fun invoke(response: Response): Response = if (done()) next(response) else this@then(response)
}

@JvmName("untilResponse")
fun ChaosPeriod.until(trigger: (Response) -> Boolean) = object : ChaosPeriod {
    private val isDone = AtomicBoolean(false)
    override fun done(): Boolean = isDone.get()
    override fun invoke(response: Response): Response {
        if (!done()) isDone.set(trigger(response))
        return this@until(response)
    }
}

fun ChaosPeriod.until(trigger: (Request) -> Boolean) = object : ChaosPeriod {
    private val isDone = AtomicBoolean(false)
    override fun done(): Boolean = isDone.get()
    override fun invoke(request: Request): Request {
        if (!done()) isDone.set(trigger(request))
        return this@until(request)
    }
}

fun ChaosPeriod.until(trigger: () -> Boolean) = object : ChaosPeriod {
    private val isDone = AtomicBoolean(false)
    override fun done(): Boolean {
        if (!isDone.get()) isDone.set(trigger())
        return isDone.get()
    }
}

fun ChaosPeriod.until(period: Duration, clock: Clock = Clock.systemUTC()) = object : ChaosPeriod {
    private val endTime by lazy { AtomicReference<Instant>(clock.instant().plus(period)) }
    override fun done() = clock.instant().isAfter(endTime.get())

}

val blockThread = Wait.until(Duration.ofSeconds(100)).then(PercentageBased(100)(BlockThread()))
val goSlow = Wait.until(Duration.ofSeconds(100)).then(Latency(Duration.ofMillis(1)))
val a = Repeat { blockThread.then(goSlow) }.until { _: Response -> true }