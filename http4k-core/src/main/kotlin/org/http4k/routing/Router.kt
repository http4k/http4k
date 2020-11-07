package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched

/**
 * Provides matching of a Request to an HttpHandler which can service it.
 */
fun interface Router {
    /**
     * Attempt to supply an HttpHandler which can service the passed request.
     */
    fun match(request: Request): RouterMatch

    fun withBasePath(new: String): Router = Prefix(new).and(this)

    fun withFilter(new: Filter): Router = this
}

sealed class RouterMatch(private val priority: Int) : Comparable<RouterMatch> {
    data class MatchingHandler(private val httpHandler: HttpHandler) : RouterMatch(0), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
    }

    object MatchedWithoutHandler : RouterMatch(1)
    object MethodNotMatched : RouterMatch(2)
    object Unmatched : RouterMatch(3)

    override fun compareTo(other: RouterMatch): Int = priority.compareTo(other.priority)
}

fun RouterMatch.and(other: RouterMatch): RouterMatch = when (this) {
    is MatchedWithoutHandler -> other
    is MatchingHandler, MethodNotMatched, Unmatched -> this
}
