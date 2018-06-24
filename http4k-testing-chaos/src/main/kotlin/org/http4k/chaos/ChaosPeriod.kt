package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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
    }
}

fun ChaosPeriod.then(next: ChaosPeriod): ChaosPeriod = object : ChaosPeriod {
    override fun done() = this@then.done().let { if (it) next.done() else it }
    override fun invoke(request: Request): Request = if (done()) next(request) else this@then(request)
    override fun invoke(response: Response): Response = if (done()) next(response) else this@then(response)
}

interface RequestPredicate : (Request) -> Boolean {
    companion object {
        fun of(): RequestPredicate  = TODO()

    }
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

fun ChaosPeriod.until(trigger: RequestPredicate) = object : ChaosPeriod {
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