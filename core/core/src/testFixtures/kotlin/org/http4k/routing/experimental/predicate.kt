package org.http4k.routing.experimental

import org.http4k.core.Request

interface Predicate {
    val description: String
    operator fun invoke(request: Request): Boolean

    companion object {
        operator fun invoke(description: String = "", predicate: (Request) -> Boolean) = object : Predicate {
            override val description: String = description
            override fun invoke(request: Request): Boolean = predicate(request)
            override fun toString(): String = description
        }
    }
}

val Any: Predicate = Predicate("any") { true }
fun Predicate.and(other: Predicate): Predicate = Predicate("($this AND $other)") { this(it) && other(it) }
fun Predicate.or(other: Predicate): Predicate = Predicate("($this OR $other)") { this(it) || other(it) }
fun Predicate.not(): Predicate = Predicate("NOT $this") { !this(it) }
