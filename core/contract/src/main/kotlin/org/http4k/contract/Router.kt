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
) : Comparable<RouterMatch> {
    data class MatchingHandler(
        private val httpHandler: HttpHandler,
    ) : RouterMatch(0), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
    }

    data object MatchedWithoutHandler : RouterMatch(1)

    data object MethodNotMatched : RouterMatch(2)

    data object Unmatched : RouterMatch(3)

    override fun compareTo(other: RouterMatch): Int = priority.compareTo(other.priority)
}
