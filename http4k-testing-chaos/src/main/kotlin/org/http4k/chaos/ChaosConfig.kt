package org.http4k.chaos

import java.time.Duration
import java.time.Duration.parse

/**
 * Handy ways to inject configuration for ChaosBehaviours into you apps.
 */
object ChaosConfig {
    object environment {
        fun LatencyRange(env: (String) -> String? = System::getenv,
                        defaultMin: Duration = Duration.ofMillis(100),
                        defaultMax: Duration = Duration.ofMillis(500),
                        minName: String = "CHAOS_LATENCY_MS_MIN",
                        maxName: String = "CHAOS_LATENCY_MS_MAX"
        ): ClosedRange<Duration> =
                (parse(minName) ?: defaultMin)..(parse(env(maxName)) ?: defaultMax)
    }
}