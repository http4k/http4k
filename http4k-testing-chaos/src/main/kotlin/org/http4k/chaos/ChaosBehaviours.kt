package org.http4k.chaos

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.chaos.ChaosBehaviours.BlockThread
import org.http4k.chaos.ChaosBehaviours.EatMemory
import org.http4k.chaos.ChaosBehaviours.KillProcess
import org.http4k.chaos.ChaosBehaviours.Latency
import org.http4k.chaos.ChaosBehaviours.NoBody
import org.http4k.chaos.ChaosBehaviours.None
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosBehaviours.SnipBody
import org.http4k.chaos.ChaosBehaviours.SnipRequestBody
import org.http4k.chaos.ChaosBehaviours.StackOverflow
import org.http4k.chaos.ChaosBehaviours.ThrowException
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.CHAOS
import org.http4k.lens.Header
import java.io.InputStream
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Duration.ofMillis
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * Encapsulates the type of bad behaviour to apply to the response.
 */
typealias Behaviour = Filter

fun Behaviour.appliedWhen(trigger: Trigger) = object : Stage {
    override fun invoke(req: Request) = if (trigger(req)) this@appliedWhen else null
    override fun toString() = trigger.toString() + " " + this@appliedWhen
}

object ChaosBehaviours {
    /**
     * Blocks the thread for a random amount of time within the allocated range.
     */
    object Latency {
        operator fun invoke(min: Duration = ofMillis(100), max: Duration = ofMillis(500)) = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = {
                val delay = ThreadLocalRandom.current()
                    .nextInt(min.toMillis().toInt(), max.toMillis().toInt())
                sleep(delay.toLong())
                next(it).with(Header.CHAOS of "Latency (${delay}ms)")
            }

            override fun toString() = "Latency (range = $min to $max)"
        }

        /**
         * Get a latency range from the environment.
         * Defaults to CHAOS_LATENCY_MS_MIN/MAX and a value of 100ms -> 500ms
         */
        fun fromEnv(
            env: (String) -> String? = System::getenv,
            defaultMin: Duration = ofMillis(100),
            defaultMax: Duration = ofMillis(500),
            minName: String = "CHAOS_LATENCY_MS_MIN",
            maxName: String = "CHAOS_LATENCY_MS_MAX"
        ) = Latency(env(minName)?.let { ofMillis(it.toLong()) } ?: defaultMin,
            env(maxName)?.let { ofMillis(it.toLong()) } ?: defaultMax)
    }

    /**
     * Throws the appropriate exception.
     */
    object ThrowException {
        operator fun invoke(e: Throwable = RuntimeException("Chaos behaviour injected!")) = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = { throw e }
            override fun toString() = "ThrowException ${e.javaClass.simpleName} ${e.localizedMessage}"
        }
    }

    /**
     * Returns an empty response with the appropriate status.
     */
    object ReturnStatus {
        operator fun invoke(status: Status = INTERNAL_SERVER_ERROR) = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = {
                Response(status).with(Header.CHAOS of "Status ${status.code}")
            }

            override fun toString() = "ReturnStatus (${status.code})"
        }
    }

    /**
     * Strips the entire body from a response.
     */
    object NoBody {
        operator fun invoke() = SnipBody { 0L }
    }

    /**
     * Strips the body from a response to a particular length.
     */
    object SnipBody {
        operator fun invoke(random: Random = Random, limitFn: (Long) -> Long) = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = {
                with(next(it)) {
                    val max = limitFn(body.length ?: 0)
                    val limit = if (max == 0L) 0L else random.nextLong(max)
                    body(body.snipTo(limit)).with(Header.CHAOS of "Snip body (${limit}b)")
                }
            }

            override fun toString() = "SnipBody"
        }
    }

    /**
     * Strips the body from a request to a particular length.
     */
    object SnipRequestBody {
        operator fun invoke(random: Random = Random, limitFn: (Long) -> Long) = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = {
                next(with(it) {
                    val max = limitFn(body.length ?: 0)
                    val limit = if (max == 0L) 0L else random.nextLong(max)
                    body(body.snipTo(limit)).with(Header.CHAOS of "Snip request body (${limit}b)")
                })
            }

            override fun toString() = "SnipRequestBody"
        }
    }

    /**
     * Allocates memory in a busy loop until an OOM occurs.
     */
    object EatMemory {
        operator fun invoke() = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = {
                mutableListOf<ByteArray>().run { while (true) this += ByteArray(1024 * 1024) }
                next(it)
            }

            override fun toString() = "EatMemory"
        }
    }

    /**
     * Allocates memory in a busy loop until an OOM occurs.
     */
    object StackOverflow {
        operator fun invoke() = object : Behaviour {
            fun overflow(): Nothing = overflow()

            override fun invoke(next: HttpHandler): HttpHandler = {
                overflow()
            }

            override fun toString() = "StackOverflow"
        }
    }

    /**
     * System exits from the process.
     */
    object KillProcess {
        operator fun invoke() = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = {
                System.exit(1)
                throw NotImplementedError()
            }

            override fun toString() = "KillProcess"
        }
    }

    /**
     * Blocks the current thread.
     */
    object BlockThread {
        operator fun invoke() = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = {
                next(it).apply { Thread.currentThread().join() }
            }

            override fun toString() = "BlockThread"
        }
    }

    /**
     * Does absolutely nothing.
     */
    object None {
        operator fun invoke() = object : Behaviour {
            override fun invoke(next: HttpHandler): HttpHandler = { next(it) }
            override fun toString() = "None"
        }
    }

    /**
     * Provide a means of modifying a ChaosBehaviour at runtime.
     */
    class Variable(var current: Behaviour = None()) : Behaviour {
        override fun invoke(next: HttpHandler): HttpHandler = {
            current.then(next)(it)
        }

        override fun toString() = current.toString()
    }
}

internal fun JsonNode.asBehaviour() = when (nonNullable<String>("type")) {
    "latency" -> Latency(nonNullable("min"), nonNullable("max"))
    "throw" -> ThrowException(RuntimeException(nonNullable<String>("message")))
    "status" -> ReturnStatus(Status(nonNullable("status"), "x-http4k-chaos"))
    "body" -> NoBody()
    "snip" -> SnipBody { it }
    "sniprequest" -> SnipRequestBody { it }
    "memory" -> EatMemory()
    "kill" -> KillProcess()
    "overflow" -> StackOverflow()
    "block" -> BlockThread()
    "none" -> None()
    else -> throw IllegalArgumentException("unknown behaviour")
}

private fun Body.snipTo(limit: Long): Body {
    var left = limit
    val original = stream
    return Body(object : InputStream() {
        override fun read() = if (left-- > 0) original.read() else -1
    }, limit)
}
