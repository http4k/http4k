package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.routing.PredicateResult.Matched
import org.http4k.routing.PredicateResult.NotMatched

interface Predicate {
    val description: String
    operator fun invoke(request: Request): PredicateResult

    companion object {

        operator fun invoke(description: String = "", notMatchedStatus: Status = Status.NOT_FOUND, predicate: (Request) -> Boolean) = object : Predicate {
            override val description: String = description
            override fun invoke(request: Request): PredicateResult = if(predicate(request)) Matched else NotMatched(notMatchedStatus)
            override fun toString(): String = description
        }
    }
}

sealed class PredicateResult{
    data object Matched: PredicateResult()
    data class NotMatched(val status:Status = Status.NOT_FOUND): PredicateResult()
}

val Any: Predicate = Predicate("any") { true }
val Fallback: Predicate = Predicate("any") { true }
fun Predicate.and(other: Predicate): Predicate = Predicate("($this AND $other)") { this(it) is Matched && other(it) is Matched }
fun Predicate.or(other: Predicate): Predicate = Predicate("($this OR $other)") { this(it) is Matched || other(it) is Matched }
fun Predicate.not(): Predicate = Predicate("NOT $this") { this(it) !is Matched  }
