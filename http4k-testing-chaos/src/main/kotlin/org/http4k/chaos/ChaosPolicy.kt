package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

interface ChaosPolicy {
    fun appliesTo(request: Request) = false
    fun appliesTo(response: Response) = false

    fun inject(behaviour: ChaosBehaviour) = let { it ->
        object : ChaosPeriod {
            override fun invoke(request: Request) = if (it.appliesTo(request)) behaviour(request) else request
            override fun invoke(response: Response) = if (it.appliesTo(response)) behaviour(response) else response
        }
    }

    companion object {
        @JvmName("OnlyResponse")
        fun Only(inject: (Response) -> Boolean) = object : ChaosPolicy {
            override fun appliesTo(response: Response) = inject(response)
        }

        fun Only(inject: (Request) -> Boolean) = object : ChaosPolicy {
            override fun appliesTo(request: Request) = inject(request)
        }

        fun Always(injectRequest: Boolean = true, injectResponse: Boolean = true) = object : ChaosPolicy {
            override fun appliesTo(request: Request) = injectRequest
            override fun appliesTo(response: Response) = injectResponse
        }

        fun PercentageBased(injectionFrequency: Int, selector: Random = ThreadLocalRandom.current()) = object : ChaosPolicy {
            override fun appliesTo(response: Response) = selector.nextInt(100) <= injectionFrequency
        }

        fun PercentageBasedFromEnv() = PercentageBased((System.getenv("CHAOS_INJECTION_FREQUENCY")?.toInt()
                ?: 50))
    }
}