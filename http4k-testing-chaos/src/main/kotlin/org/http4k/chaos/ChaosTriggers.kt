package org.http4k.chaos

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.anything
import org.http4k.chaos.ChaosTriggers.Countdown
import org.http4k.chaos.ChaosTriggers.Deadline
import org.http4k.chaos.ChaosTriggers.Delay
import org.http4k.chaos.ChaosTriggers.MatchRequest
import org.http4k.chaos.ChaosTriggers.MatchResponse
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasMethod
import org.http4k.hamkrest.hasQuery
import org.http4k.hamkrest.hasRequest
import org.http4k.hamkrest.hasResponse
import org.http4k.hamkrest.hasStatus
import org.http4k.hamkrest.hasUri
import org.http4k.hamkrest.hasUriPath
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

typealias Trigger = (HttpTransaction) -> Boolean

operator fun Trigger.not() = object : Function1<HttpTransaction, Boolean> {
    override fun invoke(p1: HttpTransaction) = !this@not(p1)
    override fun toString() = "NOT " + this@not.toString()
}

infix fun Trigger.and(that: Trigger): Trigger = object : Trigger {
    override fun invoke(p1: HttpTransaction) = this@and(p1) && that(p1)
    override fun toString() = this@and.toString() + " AND " + that.toString()
}

infix fun Trigger.or(that: Trigger): Trigger = object : Trigger {
    override fun invoke(p1: HttpTransaction) = this@or(p1) || that(p1)
    override fun toString() = this@or.toString() + " OR " + that.toString()
}

object ChaosTriggers {
    /**
     * Activates after a particular instant in time.
     */
    object Deadline {
        operator fun invoke(endTime: Instant, clock: Clock) = object : Trigger {
            override fun invoke(p1: HttpTransaction) = clock.instant().isAfter(endTime)
            override fun toString() = "Deadline ($endTime)"
        }
    }

    /**
     * Activates after a particular delay (compared to instantiation).
     */
    object Delay {
        operator fun invoke(period: Duration, clock: Clock = Clock.systemUTC()) = object : Trigger {
            private val endTime = Instant.now(clock).plus(period)
            override fun invoke(p1: HttpTransaction) = clock.instant().isAfter(endTime)
            override fun toString() = "Delay (expires $endTime)"
        }
    }

    /**
     * Activates when matching attributes of a single received request are met.
     */
    object MatchRequest {
        operator fun invoke(method: String? = null,
                            path: Regex? = null,
                            queries: Map<String, Regex>? = null,
                            headers: Map<String, Regex>? = null,
                            body: Regex? = null): Trigger {
            val headerMatchers = headers?.map { hasHeader(it.key, it.value) } ?: emptyList()
            val queriesMatchers = queries?.map { hasQuery(it.key, it.value) } ?: emptyList()
            val pathMatchers = path?.let { listOf(hasUri(hasUriPath(it))) } ?: emptyList()
            val bodyMatchers = body?.let { listOf(hasBody(it)) } ?: emptyList()
            val methodMatchers = method?.let { listOf(hasMethod(Method.valueOf(it.toUpperCase()))) } ?: emptyList()
            val all = methodMatchers + pathMatchers + queriesMatchers + headerMatchers + bodyMatchers
            val matcher = if (all.isEmpty()) hasRequest(anything) else hasRequest(all.reduce { acc, next -> acc and next })

            return object : Trigger {
                override fun invoke(p1: HttpTransaction) = matcher.asPredicate()(p1)
                override fun toString() = matcher.description
            }
        }
    }

    /**
     * Activates for a maximum number of calls.
     */
    object Countdown {
        operator fun invoke(initial: Int): Trigger = object : Trigger {
            private val count = AtomicInteger(initial)

            override fun invoke(p1: HttpTransaction) = if (count.get() > 0) { count.decrementAndGet(); true } else false

            override fun toString() = "Countdown (${count.get()} remaining)"
        }
    }

    /**
     * Activates when matching attributes of a single sent response are met.
     */
    object MatchResponse {
        operator fun invoke(status: Int? = null,
                            headers: Map<String, Regex>? = null,
                            body: Regex? = null): Trigger {
            val headerMatchers = headers?.map { hasHeader(it.key, it.value) } ?: emptyList()
            val statusMatcher = status?.let { listOf(hasStatus((Status(it, "")))) } ?: emptyList()
            val bodyMatchers = body?.let { listOf(hasBody(it)) } ?: emptyList()

            val matcher = hasResponse(
                    (headerMatchers + statusMatcher + bodyMatchers)
                            .fold<Matcher<Response>, Matcher<Response>>(anything) { acc, next -> acc and next }
            )

            return object : Trigger {
                override fun invoke(p1: HttpTransaction) = matcher.asPredicate()(p1)
                override fun toString() = matcher.description
            }
        }
    }
}

internal fun JsonNode.asTrigger(clock: Clock = Clock.systemUTC()) = when (nonNullable<String>("type")) {
    "deadline" -> Deadline(nonNullable("endTime"), clock)
    "delay" -> Delay(nonNullable("period"), clock)
    "countdown" -> Countdown(nonNullable("count"))
    "request" -> MatchRequest(asNullable("method"), asNullable("path"), toRegexMap("queries"), toRegexMap("headers"), asNullable("body"))
    "response" -> MatchResponse(asNullable("status"), toRegexMap("headers"), asNullable("body"))
    else -> throw IllegalArgumentException("unknown trigger")
}

private fun JsonNode.toRegexMap(name: String) =
        asNullable<Map<String, String>>(name)?.mapValues { it.value.toRegex() }

/**
 * Simple toggleable trigger to turn ChaosBehaviour on/off
 */
class SwitchTrigger(initialPosition: Boolean = false) : Trigger {
    private val on = AtomicBoolean(initialPosition)

    fun isActive() = on.get()

    fun toggle(newValue: Boolean? = null) = on.set(newValue ?: !on.get())

    override fun invoke(p1: HttpTransaction) = on.get()

    override fun toString() = "SwitchTrigger (active = ${on.get()})"
}