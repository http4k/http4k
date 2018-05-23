package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.filter.ChaosFilters.EnvLatencyInjectionFilter.injectionFrequency
import org.http4k.filter.ChaosFilters.EnvLatencyInjectionFilter.maxDelay
import org.http4k.filter.ChaosFilters.EnvLatencyInjectionFilter.minDelay
import java.lang.Thread.sleep
import java.util.concurrent.ThreadLocalRandom

const val LATENCY_HEADER = "X-Latency-Injected"

object ChaosFilters {
    object LatencyInjectionFilter {
        /**
         * Will inject delay to [injectionFrequency] percent of requests.
         * Delay duration from [minDelay] inclusively to [maxDelay] exclusively.
         */
        operator fun invoke(minDelay: Int, maxDelay: Int, injectionFrequency: Int): Filter = Filter { next ->
            { request ->
                val response = next(request)
                if (ThreadLocalRandom.current().nextInt(0, 100) < injectionFrequency) {
                    val delay = ThreadLocalRandom.current().nextInt(minDelay, maxDelay)
                    sleep(delay.toLong())
                    response.header(LATENCY_HEADER, delay.toString())
                } else {
                    response
                }
            }
        }
    }

    /**
     * Will inject delay to [injectionFrequency] percent of requests.
     * Delay duration from [minDelay] inclusively to [maxDelay] exclusively.
     */
    object EnvLatencyInjectionFilter {
        private val minDelay = (System.getenv("CHAOS_LATENCY_MS_MIN_VALUE") ?: "100").toInt()
        private val maxDelay = (System.getenv("CHAOS_LATENCY_MS_MAX_VALUE") ?: "1000").toInt()
        private val injectionFrequency = (System.getenv("CHAOS_LATENCY_INJECTION_FREQUENCY") ?: "50").toInt()

        operator fun invoke(): Filter = Filter { next ->
            { request ->
                val response = next(request)
                if (ThreadLocalRandom.current().nextInt(0, 100) <= injectionFrequency) {
                    val delay = ThreadLocalRandom.current().nextInt(minDelay, maxDelay)
                    sleep(delay.toLong())
                    response.header(LATENCY_HEADER, delay.toString())
                } else {
                    response
                }
            }
        }
    }
}
