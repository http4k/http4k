package org.http4k.chaos

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Properties

class ChaosConfigTest {
    @Test
    fun `create a latency range from an environment`() {
        val props = Properties().apply {
            put("CHAOS_LATENCY_MS_MIN", "1000")
            put("CHAOS_LATENCY_MS_MAX", "1000000")
        }

        ChaosConfig.env.LatencyRange(props::getProperty) shouldMatch
                equalTo((Duration.ofSeconds(1)..Duration.ofSeconds(1000)))
    }

    @Test
    fun `default latency range`() {
        ChaosConfig.env.LatencyRange() shouldMatch
                equalTo((Duration.ofMillis(100)..Duration.ofMillis(500)))
    }

    @Test
    fun `create a percentage from an environment`() {
        val props = Properties().apply {
            put("CHAOS_PERCENTAGE", "100")
        }

        ChaosConfig.env.Percentage(props::getProperty) shouldMatch equalTo(100)
    }

    @Test
    fun `default percentage`() {
        ChaosConfig.env.Percentage() shouldMatch equalTo((50))
    }
}