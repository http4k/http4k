package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

interface ChaosPolicy {
    fun shouldInject(request: Request) = false
    fun shouldInject(response: Response) = false

    operator fun invoke(behaviour: ChaosBehaviour) = let { it ->
        object : ChaosPeriod {
            override fun invoke(request: Request) = if (it.shouldInject(request)) behaviour(request) else request
            override fun invoke(response: Response) = if (it.shouldInject(response)) behaviour(response) else response
        }
    }

    companion object {
        @JvmName("OnlyResponse")
        fun Only(inject: (Response) -> Boolean) = object : ChaosPolicy {
            override fun shouldInject(response: Response) = inject(response)
        }

        fun Only(inject: (Request) -> Boolean) = object : ChaosPolicy {
            override fun shouldInject(request: Request) = inject(request)
        }

        fun Always(injectRequest: Boolean = true, injectResponse: Boolean = true) = object : ChaosPolicy {
            override fun shouldInject(request: Request) = injectRequest
            override fun shouldInject(response: Response) = injectResponse
        }

        fun PercentageBased(injectionFrequency: Int, selector: Random = ThreadLocalRandom.current()) = object : ChaosPolicy {
            override fun shouldInject(response: Response) = selector.nextInt(100) <= injectionFrequency
        }

        fun PercentageBasedFromEnv() = PercentageBased((System.getenv("CHAOS_INJECTION_FREQUENCY")?.toInt()
                ?: 50))
    }
}