package org.http4k.contract

import org.http4k.contract.RouterMatch.MatchedWithoutHandler
import org.http4k.contract.RouterMatch.MatchingHandler
import org.http4k.contract.RouterMatch.MethodNotMatched
import org.http4k.contract.RouterMatch.Unmatched
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.RouterDescription

/**
 * The result of a matching operation. May or may not contain a matched HttpHandler.
 */
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

internal fun RouterMatch.and(other: RouterMatch): RouterMatch = when (this) {
    is MatchedWithoutHandler -> other
    is MethodNotMatched, is MatchingHandler -> when (other) {
        is MatchingHandler, is MatchedWithoutHandler, is MethodNotMatched -> this
        is Unmatched -> other
    }

    is Unmatched -> this
}
