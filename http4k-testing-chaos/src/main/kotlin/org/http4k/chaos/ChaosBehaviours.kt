package org.http4k.chaos

import org.http4k.core.Body.Companion.EMPTY
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
 * Encapsulates the type of bad behaviour to apply to the response.
 */
interface ChaosBehaviour {
    operator fun invoke(tx: HttpTransaction): Response
}

abstract class SerializableBehaviour(val type: String) : ChaosBehaviour

object ChaosBehaviours {
    /**
     * Blocks the thread for a random amount of time within the allocated range.
     */
    data class Latency(val min: Duration = ofMillis(100), val max: Duration = ofMillis(500)) : SerializableBehaviour("latency") {
        constructor(latencyRange: ClosedRange<Duration>) : this(latencyRange.start, latencyRange.endInclusive)

        override fun invoke(tx: HttpTransaction): Response {
            val delay = ThreadLocalRandom.current()
                    .nextInt(min.toMillis().toInt(), max.toMillis().toInt())
            sleep(delay.toLong())
            return tx.response.with(Header.Common.CHAOS of "Latency (${delay}ms)")
        }

        override fun toString() = "Latency (range = $min to $max)"

        companion object {
            /**
             * Get a latency range from the environment.
             * Defaults to CHAOS_LATENCY_MS_MIN/MAX and a value of 100ms -> 500ms
             */
            fun fromEnv(env: (String) -> String? = System::getenv,
                        defaultMin: Duration = Duration.ofMillis(100),
                        defaultMax: Duration = Duration.ofMillis(500),
                        minName: String = "CHAOS_LATENCY_MS_MIN",
                        maxName: String = "CHAOS_LATENCY_MS_MAX"
            ) = Latency((env(minName)?.let { Duration.ofMillis(it.toLong()) } ?: defaultMin)..
                    (env(maxName)?.let { Duration.ofMillis(it.toLong()) } ?: defaultMax))
        }
    }

    /**
     * Throws the appropriate exception.
     */
    data class ThrowException private constructor(val message: String, private val e: Throwable?) : SerializableBehaviour("throw") {
        constructor(e: Throwable = Exception("Chaos behaviour injected!")) : this(e.localizedMessage, e)

        private fun fallback() = e ?: Exception(message)

        override fun invoke(tx: HttpTransaction): Nothing = throw fallback()
        override fun toString() = "ThrowException ${fallback().javaClass.simpleName} ${fallback().message}"
    }


    /**
     * Returns an empty response with the appropriate status.
     */
    data class ReturnStatus(private val status: Status = INTERNAL_SERVER_ERROR) : SerializableBehaviour("status") {
        override fun invoke(tx: HttpTransaction) = Response(status).with(Header.Common.CHAOS of "Status ${status.code}")
        override fun toString() = "ReturnStatus (${status.code})"
    }

    /**
     * Strips the body from a response.
     */
    object NoBody : SerializableBehaviour("body") {
        override fun invoke(tx: HttpTransaction) = tx.response.body(EMPTY).with(Header.Common.CHAOS of "No body")
        override fun toString() = "NoBody"
    }

    /**
     * Allocates memory in a busy loop until an OOM occurs.
     */
    object EatMemory : SerializableBehaviour("memory") {
        override fun invoke(tx: HttpTransaction) = tx.response.apply {
            mutableListOf<ByteArray>().let { while (true) it += ByteArray(1024 * 1024) }
        }

        override fun toString() = "EatMemory"
    }

    /**
     * Allocates memory in a busy loop until an OOM occurs.
     */
    object StackOverflow : SerializableBehaviour("overflow") {
        override fun invoke(tx: HttpTransaction): Response {
            fun overflow(): Unit = overflow()
            return tx.response.apply { overflow() }
        }

        override fun toString() = "StackOverflow"
    }

    /**
     * System exits from the process.
     */
    object KillProcess : SerializableBehaviour("kill") {
        override fun invoke(tx: HttpTransaction) = tx.response.apply { System.exit(1) }
        override fun toString() = "KillProcess"
    }

    /**
     * Blocks the current thread.
     */
    object BlockThread : SerializableBehaviour("block") {
        override fun invoke(tx: HttpTransaction) = tx.response.apply { Thread.currentThread().join() }
        override fun toString() = "BlockThread"
    }

    /**
     * Does absolutely nothing.
     */
    val None = object : SerializableBehaviour("none") {
        override fun invoke(tx: HttpTransaction) = tx.response
        override fun toString() = "None"
    }

    /**
     * Provide a means of modifying a ChaosBehaviour at runtime.
     */
    data class Variable(var current: ChaosBehaviour = None) : ChaosBehaviour {
        override fun invoke(tx: HttpTransaction) = current(tx)
        override fun toString() = "Variable [$current]"
    }
}