package org.http4k.chaos

import org.http4k.core.HttpMessage
import java.lang.Thread.sleep
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

interface ChaosBehaviour {
    fun inject(message: HttpMessage)
}

class AdditionalLatencyBehaviour(
    private val minDelay: Duration,
    private val maxDelay: Duration
) : ChaosBehaviour {
    override fun inject(message: HttpMessage) {
        val delay = ThreadLocalRandom.current().nextInt(minDelay.toMillis().toInt(), maxDelay.toMillis().toInt())
        sleep(delay.toLong())
    }

    companion object {
        fun fromEnv(): AdditionalLatencyBehaviour {
            val minDelay = Duration.parse(System.getenv("CHAOS_LATENCY_MS_MIN") ?: "PT0.1S")
            val maxDelay = Duration.parse(System.getenv("CHAOS_LATENCY_MS_MAX") ?: "PT0.5S")
            return AdditionalLatencyBehaviour(minDelay, maxDelay)
        }
    }
}

class ChaosException(message: String) : Exception(message)

class ExceptionThrowingBehaviour : ChaosBehaviour {
    override fun inject(message: HttpMessage) {
        throw ChaosException("Chaos behaviour injected!")
    }
}
