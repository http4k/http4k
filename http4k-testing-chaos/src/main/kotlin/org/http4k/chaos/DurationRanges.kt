package org.http4k.chaos

import java.time.Duration

object DurationRanges {
    fun env(env: (String) -> String? = System::getenv,
            defaultMin: Duration = Duration.ofMillis(100),
            defaultMax: Duration = Duration.ofMillis(500),
            minName: String = "CHAOS_LATENCY_MS_MIN",
            maxName: String = "CHAOS_LATENCY_MS_MAX"
            ): ClosedRange<Duration> =
            (Duration.parse(minName)
                    ?: defaultMin)..(Duration.parse(env(maxName)) ?: defaultMax)
}