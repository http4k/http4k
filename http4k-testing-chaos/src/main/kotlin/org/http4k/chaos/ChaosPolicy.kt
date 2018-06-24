package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

interface ChaosPolicy {
    fun shouldInject(request: Request): Boolean = false
    fun shouldInject(response: Response): Boolean = false

    companion object {
        fun Always(injectRequest: Boolean = true, injectResponse: Boolean = true) = object : ChaosPolicy {
            override fun shouldInject(request: Request) = injectRequest
            override fun shouldInject(response: Response) = injectResponse
        }

        fun PercentageBased(injectionFrequency: Int, selector: Random = ThreadLocalRandom.current()) = object : ChaosPolicy {
            override fun shouldInject(response: Response) = selector.nextInt(100) <= injectionFrequency
        }

        fun PercentageBasedFromEnv() = PercentageBased((System.getenv("CHAOS_INJECTION_FREQUENCY")?.toInt() ?: 50))
    }
}