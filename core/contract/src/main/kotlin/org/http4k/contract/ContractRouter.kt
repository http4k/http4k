package org.http4k.contract

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.RouterDescription
import org.http4k.routing.RouterDescription.Companion.unavailable

interface ContractRouter {
    fun match(request: Request): ContractRouterMatch

    val description get() = "unavailable"
}

sealed class ContractRouterMatch(
    val priority: Int,
    open val description: RouterDescription
) : Comparable<ContractRouterMatch> {
    data class MatchingHandler(
        override val description: RouterDescription,
        private val httpHandler: HttpHandler,
    ) : ContractRouterMatch(0, description), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
    }

    data object MatchedWithoutHandler : ContractRouterMatch(1, unavailable)

    data object MethodNotMatched : ContractRouterMatch(2, unavailable)

    data object Unmatched : ContractRouterMatch(3, unavailable)

    override fun compareTo(other: ContractRouterMatch): Int = priority.compareTo(other.priority)
}
