package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.with
import java.io.PrintStream
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

const val LATENCY_HEADER = "X-Latency-Injected"

object ChaosFilters {
    object LatencyInjectionFilter {
        operator fun invoke(minDelay: Int, maxDelay: Int, injectionFrequency: Int): Filter = ResponseFilters.Tap { response ->
            if (ThreadLocalRandom.current().nextInt(1, 101) <= injectionFrequency) {
                val delay = ThreadLocalRandom.current().nextInt(minDelay, maxDelay)
                TimeUnit.MILLISECONDS.sleep(delay.toLong())
                response.header(LATENCY_HEADER, delay.toString())
            }
        }
    }

    /**
     * Will inject delay to [injectionFrequency] percent of requests.
     * Delay duration from [minDelay] to [maxDelay] both inclusive.
     */
    class LatencyAssaultFilter(
        private val enabled: Boolean,
        private val minDelay: Int,
        private val maxDelay: Int,
        private val injectionFrequency: Int
    ) {
        operator fun invoke(): Filter = RequestFilters.Tap { request ->
            if (enabled) {
                if (ThreadLocalRandom.current().nextInt(0, 101) <= injectionFrequency) {
                    val delay = ThreadLocalRandom.current().nextInt(minDelay, maxDelay)
                    TimeUnit.MILLISECONDS.sleep(delay.toLong())
                    request.header(LATENCY_HEADER, delay.toString())
                }
            }
        }

        companion object {
            fun buildFromEnvVariables(): LatencyAssaultFilter {
                val enabled = (System.getenv("CHAOS_LATENCY_ASSAULT_ENABLED") ?: "false").toBoolean()
                val minDelay = (System.getenv("CHAOS_LATENCY_MS_MIN_VALUE") ?: "100").toInt()
                val maxDelay = (System.getenv("CHAOS_LATENCY_MS_MAX_VALUE") ?: "1000").toInt()
                val injectionFrequency = (System.getenv("CHAOS_LATENCY_INJECTION_FREQUENCY") ?: "50").toInt()
                return LatencyAssaultFilter(enabled, minDelay, maxDelay, injectionFrequency)
            }
        }
    }
}
