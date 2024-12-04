package org.http4k.contract

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.RouterDescription


interface ContractRouter {
    fun match(request: Request): ContractRouterMatch

    val description: RouterDescription get() = RouterDescription.unavailable
}

sealed class ContractRouterMatch(
    val priority: Int,
) : Comparable<ContractRouterMatch> {
    data class MatchingHandler(
        private val httpHandler: HttpHandler,
    ) : ContractRouterMatch(0), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
    }

    data object MatchedWithoutHandler : ContractRouterMatch(1)

    data object MethodNotMatched : ContractRouterMatch(2)

    data object Unmatched : ContractRouterMatch(3)

    override fun compareTo(other: ContractRouterMatch): Int = priority.compareTo(other.priority)
}
