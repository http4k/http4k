package org.http4k.chaos

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Duration.ofMillis
import java.time.Duration.parse
import java.util.concurrent.ThreadLocalRandom

interface ChaosBehaviour {
    val description: String
        get() = this.javaClass.name

    fun inject(request: Request) = request
    fun inject(response: Response) = response

    companion object {
        fun Latency(minDelay: Duration, maxDelay: Duration) = object : ChaosBehaviour {
            override val description = "Latency"

            override fun inject(response: Response): Response {
                val delay = ThreadLocalRandom.current().nextInt(minDelay.toMillis().toInt(), maxDelay.toMillis().toInt())
                sleep(delay.toLong())
                return response
            }
        }

        fun ExtraLatencyFromEnv(): ChaosBehaviour = Latency(
                parse(System.getenv("CHAOS_LATENCY_MS_MIN")) ?: ofMillis(100),
                parse(System.getenv("CHAOS_LATENCY_MS_MAX")) ?: ofMillis(500))

        fun ThrowException(e: Exception = ChaosException("Chaos behaviour injected!")) = object : ChaosBehaviour {
            override val description = "Exception"

            override fun inject(response: Response) = throw e
        }

        fun KillProcess() = object : ChaosBehaviour {
            override fun inject(response: Response) = response.apply { System.exit(1) }
        }

        fun BlockThread() = object : ChaosBehaviour {
            override fun inject(response: Response) = response.apply { Thread.currentThread().join() }
        }

        fun ReturnStatus(status: Status = INTERNAL_SERVER_ERROR) = object : ChaosBehaviour {
            override val description = status.description

            override fun inject(response: Response) = Response(status).headers(response.headers)
        }
    }
}

class ChaosException(message: String) : Exception(message)
