package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched

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
        override fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch =
            copy(description = description, subMatches = fromMatches)
    }

    data class MatchedWithoutHandler(
        override val description: RouterDescription,
        override val subMatches: List<RouterMatch> = listOf()
    ) : RouterMatch(1, description, subMatches) {
        override fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch =
            copy(description = description, subMatches = fromMatches)
    }

    data class MethodNotMatched(
        override val description: RouterDescription,
        override val subMatches: List<RouterMatch> = listOf()
    ) : RouterMatch(2, description, subMatches) {
        override fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch =
            copy(description = description, subMatches = fromMatches)
    }

    data class Unmatched(
        override val description: RouterDescription,
        override val subMatches: List<RouterMatch> = listOf()
    ) : RouterMatch(3, description, subMatches) {
        override fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch =
            copy(description = description, subMatches = fromMatches)
    }

    override fun compareTo(other: RouterMatch): Int = priority.compareTo(other.priority)
    abstract fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch
}

internal fun RouterMatch.and(other: RouterMatch): RouterMatch = when (this) {
    is MatchedWithoutHandler -> other
    is MethodNotMatched, is MatchingHandler -> when (other) {
        is MatchingHandler, is MatchedWithoutHandler, is MethodNotMatched -> this
        is Unmatched -> other
    }

    is Unmatched -> this
}
