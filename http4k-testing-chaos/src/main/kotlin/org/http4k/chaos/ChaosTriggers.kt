package org.http4k.chaos

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.anything
import org.http4k.core.HttpTransaction
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
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

typealias ChaosTrigger = (HttpTransaction) -> Boolean

abstract class SerializableTrigger(val type: String) {
    abstract operator fun invoke(clock: Clock): ChaosTrigger
}

object ChaosTriggers {
    /**
     * Activates after a particular instant in time.
     */
    data class Deadline(val endTime: Instant) : SerializableTrigger("deadline") {
        override operator fun invoke(clock: Clock) = Instant.now(clock).let {
            object : ChaosTrigger {
                override fun invoke(p1: HttpTransaction) = clock.instant().isAfter(it)
                override fun toString() = "Deadline ($it)"
            }
        }
    }

    /**
     * Activates after a particular delay (compared to instantiation).
     */
    data class Delay(val endTime: Instant) : SerializableTrigger("delay") {
        constructor(period: Duration, clock: Clock = Clock.systemUTC()) : this(Instant.now(clock).plus(period))

        override operator fun invoke(clock: Clock): ChaosTrigger = object : ChaosTrigger {
            override fun invoke(p1: HttpTransaction) = clock.instant().isAfter(endTime)
            override fun toString() = "Delay (expires $endTime)"
        }
    }

    abstract class HttpTransactionTrigger(type: String, matcher: Matcher<HttpTransaction>)
        : SerializableTrigger(type), Matcher<HttpTransaction> by matcher {
        override operator fun invoke(clock: Clock) = object : ChaosTrigger {
            override fun invoke(p1: HttpTransaction) = this@HttpTransactionTrigger.asPredicate()(p1)
            override fun toString() = this@HttpTransactionTrigger.description
        }
    }

    data class MatchRequest(val path: Regex?,
                            val headers: Map<String, Regex>?,
                            val queries: Map<String, String>?,
                            val body: Regex?) : HttpTransactionTrigger("request", {
        val headerMatchers = headers?.map { hasHeader(it.key, it.value) } ?: emptyList()
        val queriesMatchers = queries?.map { hasQuery(it.key, it.value) } ?: emptyList()
        val pathMatchers = path?.let { listOf(hasUri(hasUriPath(it))) } ?: emptyList()
        val bodyMatchers = body?.let { listOf(hasBody(it)) } ?: emptyList()
        hasRequest(
                (headerMatchers + queriesMatchers + pathMatchers + bodyMatchers)
                        .fold<Matcher<Request>, Matcher<Request>>(anything) { acc, next -> acc.and(next) }
        )
    }())

    data class MatchResponse(val status: Int?,
                             val headers: Map<String, Regex>?,
                             val body: Regex?) : HttpTransactionTrigger("response", {
        val headerMatchers = headers?.map { hasHeader(it.key, it.value) } ?: emptyList()
        val statusMatcher = status?.let { listOf(hasStatus((Status(it, "")))) } ?: emptyList()
        val bodyMatchers = body?.let { listOf(hasBody(it)) } ?: emptyList()

        hasResponse(
                (headerMatchers + statusMatcher + bodyMatchers)
                        .fold<Matcher<Response>, Matcher<Response>>(anything) { acc, next -> acc.and(next) }
        )
    }())
}

/**
 * Simple toggleable trigger to turn ChaosBehaviour on/off
 */
class SwitchTrigger(initialPosition: Boolean = false) : ChaosTrigger {
    private val on = AtomicBoolean(initialPosition)

    fun isActive() = on.get()

    fun toggle(newValue: Boolean? = null) = on.set(newValue ?: !on.get())

    override fun invoke(p1: HttpTransaction) = on.get()

    override fun toString() = "SwitchTrigger (active = ${on.get()})"

}

operator fun ChaosTrigger.not() = object : Function1<HttpTransaction, Boolean> {
    override fun invoke(p1: HttpTransaction): Boolean = !this@not(p1)
    override fun toString() = "NOT " + this@not.toString()
}