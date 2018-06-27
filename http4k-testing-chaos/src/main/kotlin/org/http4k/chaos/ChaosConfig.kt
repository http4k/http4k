package org.http4k.chaos

import java.time.Duration

/**
 * Handy ways to inject configuration for ChaosBehaviours into your apps.
 */
object ChaosConfig {
    object env {
        /**
         * Get a latency range from the environment.
         * Defaults to CHAOS_LATENCY_MS_MIN/MAX and a value of 100ms -> 500ms
         */
        fun LatencyRange(env: (String) -> String? = System::getenv,
                         defaultMin: Duration = Duration.ofMillis(100),
                         defaultMax: Duration = Duration.ofMillis(500),
                         minName: String = "CHAOS_LATENCY_MS_MIN",
                         maxName: String = "CHAOS_LATENCY_MS_MAX"
        ): ClosedRange<Duration> {
            return (env(minName)?.let { Duration.ofMillis(it.toLong()) } ?: defaultMin)..
                    (env(maxName)?.let { Duration.ofMillis(it.toLong()) } ?: defaultMax)
        }

        /**
         * Get a percentage from the environment.
         * Defaults to CHAOS_PERCENTAGE and a value of 50%
         */
        fun Percentage(env: (String) -> String? = System::getenv,
                       defaultPercentage: Int = 50,
                       name: String = "CHAOS_PERCENTAGE"
        ): Int = env(name)?.let(Integer::parseInt) ?: defaultPercentage
    }
}