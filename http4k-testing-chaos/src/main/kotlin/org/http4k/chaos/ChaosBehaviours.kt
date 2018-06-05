package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.lang.Thread.sleep
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

interface ChaosBehaviour {
    val description: String
        get() = this.javaClass.name

    fun inject(request: Request): Request = request
    fun inject(response: Response): Response = response
}

class AdditionalLatencyBehaviour(
    private val minDelay: Duration,
    private val maxDelay: Duration
) : ChaosBehaviour {
    override val description = "Latency"

    override fun inject(response: Response): Response {
        val delay = ThreadLocalRandom.current().nextInt(minDelay.toMillis().toInt(), maxDelay.toMillis().toInt())
        sleep(delay.toLong())
        return response
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
    override val description = "Exception"

    override fun inject(response: Response): Response {
        throw ChaosException("Chaos behaviour injected!")
    }
}

class ProcessKillingBehaviour : ChaosBehaviour {
    override fun inject(response: Response): Response {
        System.exit(1)
        return response
    }
}

class UnlimitedThreadBlockBehaviour : ChaosBehaviour {
    override fun inject(response: Response): Response {
        sleep(Long.MAX_VALUE)
        return response
    }
}

class InternalServerErrorResponseBehaviour : ChaosBehaviour {
    override val description = "Internal Server Error"

    override fun inject(response: Response): Response {
        return Response(Status.INTERNAL_SERVER_ERROR).headers(response.headers)
    }
}
