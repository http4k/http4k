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

    companion object {
        fun ExtraLatency(minDelay: Duration, maxDelay: Duration) = object : ChaosBehaviour {
            override val description = "Latency"

            override fun inject(response: Response): Response {
                val delay =
                    ThreadLocalRandom.current().nextInt(minDelay.toMillis().toInt(), maxDelay.toMillis().toInt())
                sleep(delay.toLong())
                return response
            }
        }

        fun ExtraLatencyFromEnv(): ChaosBehaviour {
            val minDelay = Duration.parse(System.getenv("CHAOS_LATENCY_MS_MIN") ?: "PT0.1S")
            val maxDelay = Duration.parse(System.getenv("CHAOS_LATENCY_MS_MAX") ?: "PT0.5S")
            return ExtraLatency(minDelay, maxDelay)
        }

        fun ThrowException(e: Exception = ChaosException("Chaos behaviour injected!")) = object : ChaosBehaviour {
            override val description = "Exception"

            override fun inject(response: Response): Response {
                throw e
            }
        }

        fun KillProcess() = object : ChaosBehaviour {
            override fun inject(response: Response): Response {
                System.exit(1)
                return response
            }
        }

        fun BlockThread() = object : ChaosBehaviour {
            override fun inject(response: Response): Response {
                sleep(Long.MAX_VALUE)
                return response
            }
        }

        fun ReturnInternalServerError() = object : ChaosBehaviour {
            override val description = "Internal Server Error"

            override fun inject(response: Response): Response {
                return Response(Status.INTERNAL_SERVER_ERROR).headers(response.headers)
            }
        }
    }
}

class ChaosException(message: String) : Exception(message)
