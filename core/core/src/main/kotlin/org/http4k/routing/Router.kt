package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched

interface Router {
    val description: RouterDescription
    operator fun invoke(request: Request): RoutingResult

    companion object {

        operator fun invoke(
            description: String,
            notMatchedStatus: Status = NOT_FOUND,
            predicate: (Request) -> Boolean
        ) = object : Router {
            override val description = RouterDescription(description)
            override fun invoke(request: Request): RoutingResult =
                if (predicate(request)) Matched(this.description) else NotMatched(notMatchedStatus, this.description)

            override fun toString(): String = description
        }
    }
}

sealed interface RoutingResult {
    val description: RouterDescription

    data class Matched(override val description: RouterDescription) : RoutingResult
    data class NotMatched(val status: Status = NOT_FOUND, override val description: RouterDescription) : RoutingResult
}

val All = Router("all") { true }

val orElse = All

fun Router.and(other: Router) = when (this) {
    All -> other
    else -> when (other) {
        All -> this
        else -> Router("($this AND $other)") { this(it) is Matched && other(it) is Matched }
    }
}

fun Router.or(other: Router) = when (this) {
    All -> other
    else -> when (other) {
        All -> this
        else -> Router("($this OR $other)") { this(it) is Matched || other(it) is Matched }
    }
}

fun Router.not(): Router = Router("NOT $this") { this(it) !is Matched }

fun ((Request) -> Boolean).asRouter(name: String = "") = Router(name, NOT_FOUND, this)

