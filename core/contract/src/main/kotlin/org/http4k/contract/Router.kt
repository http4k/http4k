package org.http4k.contract

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.RouterDescription


interface Router {
    fun match(request: Request): RouterMatch

    val description: RouterDescription get() = RouterDescription.unavailable
}

sealed class RouterMatch(
    val priority: Int,
    open val description: RouterDescription,
    open val subMatches: List<RouterMatch>
) : Comparable<RouterMatch> {
    data class MatchingHandler(
        private val httpHandler: HttpHandler,
        override val description: RouterDescription,
        override val subMatches: List<RouterMatch> = listOf()
    ) : RouterMatch(0, description, subMatches), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
    }

    data class MatchedWithoutHandler(
        override val description: RouterDescription,
        override val subMatches: List<RouterMatch> = listOf()
    ) : RouterMatch(1, description, subMatches)

    data class MethodNotMatched(
        override val description: RouterDescription,
        override val subMatches: List<RouterMatch> = listOf()
    ) : RouterMatch(2, description, subMatches)

    data class Unmatched(
        override val description: RouterDescription,
        override val subMatches: List<RouterMatch> = listOf()
    ) : RouterMatch(3, description, subMatches)

    override fun compareTo(other: RouterMatch): Int = priority.compareTo(other.priority)
}
