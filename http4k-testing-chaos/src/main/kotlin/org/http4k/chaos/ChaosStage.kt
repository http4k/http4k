package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Defines a period during which a particular ChaosBehaviour to be active.
 */
interface ChaosStage {
    fun done(): Boolean = false
    operator fun invoke(request: Request) = request
    operator fun invoke(response: Response) = response

    companion object {
        fun Repeat(stage: () -> ChaosStage): ChaosStage = object : ChaosStage {
            private val current by lazy { AtomicReference(stage()) }
            override fun done(): Boolean {
                if (current.get().done())
                    current.set(stage())
                return current.get().done()
            }

            override fun invoke(request: Request) = current.get()(request)
            override fun invoke(response: Response) = current.get()(response)
        }

        object Wait : ChaosStage
    }
}

fun ChaosStage.then(next: ChaosStage): ChaosStage = object : ChaosStage {
    override fun done() = this@then.done().let { if (it) next.done() else it }
    override fun invoke(request: Request): Request = if (done()) next(request) else this@then(request)
    override fun invoke(response: Response): Response = if (done()) next(response) else this@then(response)
}

@JvmName("untilResponse")
fun ChaosStage.until(trigger: Trigger<Response>) = object : ChaosStage {
    private val isDone = AtomicBoolean(false)
    override fun done(): Boolean = isDone.get()
    override fun invoke(response: Response): Response {
        if (!done()) isDone.set(trigger(response))
        return this@until(response)
    }
}

fun ChaosStage.until(trigger: Trigger<Request>) = object : ChaosStage {
    private val isDone = AtomicBoolean(false)
    override fun done(): Boolean = isDone.get()
    override fun invoke(request: Request): Request {
        if (!done()) isDone.set(trigger(request))
        return this@until(request)
    }
}
