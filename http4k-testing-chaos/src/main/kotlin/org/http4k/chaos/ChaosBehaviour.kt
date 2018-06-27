package org.http4k.chaos

import org.http4k.core.Body
import org.http4k.core.HttpTransaction
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.with
import org.http4k.lens.Header
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Duration.ofMillis
import java.util.concurrent.ThreadLocalRandom

val Header.Common.CHAOS; get() = Header.required("x-http4k-chaos")

/**
 * Encapsulates the type of bad behaviour to apply to the request/response.
 */
interface ChaosBehaviour {
    operator fun invoke(tx: HttpTransaction) = tx.response

    companion object {
        fun Latency(latencyRange: ClosedRange<Duration> = ofMillis(100)..ofMillis(500)) = object : ChaosBehaviour {
            override fun invoke(tx: HttpTransaction): Response {
                val delay = ThreadLocalRandom.current()
                        .nextInt(latencyRange.start.toMillis().toInt(), latencyRange.endInclusive.toMillis().toInt())
                sleep(delay.toLong())
                return tx.response.with(Header.Common.CHAOS of "Latency (${delay}ms)")
            }
        }

        fun ThrowException(e: Throwable = Exception("Chaos behaviour injected!")) = object : ChaosBehaviour {
            override fun invoke(tx: HttpTransaction) = throw e
        }

        fun ReturnStatus(status: Status = INTERNAL_SERVER_ERROR) = object : ChaosBehaviour {
            override fun invoke(tx: HttpTransaction) = Response(status).with(Header.Common.CHAOS of "Status ${status.code}")
        }

        fun NoBody() = object : ChaosBehaviour {
            override fun invoke(tx: HttpTransaction) = tx.response.body(Body.EMPTY).with(Header.Common.CHAOS of "No body")
        }

        @Suppress("unused") // untestable
        fun EatMemory() = object : ChaosBehaviour {
            override fun invoke(tx: HttpTransaction) = tx.response.apply {
                mutableListOf<ByteArray>().let { while (true) it += ByteArray(1024 * 1024) }
            }
        }

        @Suppress("unused") // untestable
        fun StackOverflow() = object : ChaosBehaviour {
            override fun invoke(tx: HttpTransaction): Response {
                fun overflow(): Unit = overflow()
                return tx.response.apply { overflow() }
            }
        }

        @Suppress("unused") // untestable
        fun KillProcess() = object : ChaosBehaviour {
            override fun invoke(tx: HttpTransaction) = tx.response.apply { System.exit(1) }
        }

        @Suppress("unused") // untestable
        fun BlockThread() = object : ChaosBehaviour {
            override fun invoke(tx: HttpTransaction) = tx.response.apply { Thread.currentThread().join() }
        }
    }
}
