package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

/**
 * Determines whether or not to apply a particular type of ChaosBehaviour to a request/response.
 */
interface ChaosPolicy {
    fun appliesTo(request: Request) = false
    fun appliesTo(response: Response) = false

    fun inject(behaviour: ChaosBehaviour) = let { it ->
        object : ChaosStage {
            override fun invoke(request: Request) = if (it.appliesTo(request)) behaviour(request) else request
            override fun invoke(response: Response) = if (it.appliesTo(response)) behaviour(response) else response
        }
    }

    companion object {
        @JvmName("OnlyResponse")
        fun Only(trigger: Trigger<Response>) = object : ChaosPolicy {
            override fun appliesTo(response: Response) = trigger(response)
        }

        fun Only(trigger: Trigger<Request>) = object : ChaosPolicy {
            override fun appliesTo(request: Request) = trigger(request)
        }

        fun Always(injectRequest: Boolean = true, injectResponse: Boolean = true) = object : ChaosPolicy {
            override fun appliesTo(request: Request) = injectRequest
            override fun appliesTo(response: Response) = injectResponse
        }

        fun PercentageBased(injectionFrequency: Int, selector: Random = ThreadLocalRandom.current()) = object : ChaosPolicy {
            override fun appliesTo(response: Response) = selector.nextInt(100) <= injectionFrequency
        }

        fun PercentageBasedFromEnv() = PercentageBased((System.getenv("CHAOS_INJECTION_FREQUENCY")?.toInt() ?: 50))
    }
}