package org.http4k

import org.http4k.contract.BasePath
import org.http4k.contract.ContractRenderer
import org.http4k.contract.Security
import org.http4k.contract.isIn
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header
import org.http4k.routing.RoutingHttpHandler

class Contract internal constructor(val httpHandler: Contract.Companion.Handler) : RoutingHttpHandler {
    override fun match(request: Request): HttpHandler? = httpHandler.match(request)

    override fun invoke(request: Request): Response = httpHandler(request)

    override fun withBasePath(new: String): Contract = Contract(httpHandler.withBasePath(new))
    override fun withFilter(new: Filter): RoutingHttpHandler = Contract(httpHandler.withFilter(new))

    companion object {
        internal data class Handler(private val renderer: ContractRenderer,
                                    private val security: Security,
                                    private val descriptionPath: String,
                                    private val rootAsString: String = "",
                                    private val routes: List<ServerRoute2> = emptyList(),
                                    private val filter: Filter = ServerFilters.CatchLensFailure
        ) : HttpHandler {
            private val contractRoot = BasePath(rootAsString)
            internal fun withFilter(new: Filter) = copy(filter = filter.then(new))
            internal fun withBasePath(new: String) = copy(rootAsString = new + rootAsString)

            private val handler: HttpHandler = { match(it)?.invoke(it) ?: Response(Status.NOT_FOUND.description("Route not found")) }

            override fun invoke(request: Request): Response = handler(request)

            private val descriptionRoute = GET to PathDef0 { BasePath("$it$descriptionPath") } bindTo { Response(Status.OK) }

            private val routers = routes
                .map { it.router(contractRoot) to security.filter.then(identify(it)).then(filter) }
                .plus(descriptionRoute.router(contractRoot) to identify(descriptionRoute).then(filter))

            private val noMatch: HttpHandler? = null

            fun match(request: Request): HttpHandler? =
                if (request.isIn(contractRoot)) {
                    routers.fold(noMatch, { memo, (router, routeFilter) ->
                        memo ?: router.match(request)?.let { routeFilter.then(it) }
                    })
                } else null

            private fun identify(route: ServerRoute2): Filter =
                route.describeFor(contractRoot).let { routeIdentity ->
                    Filter { next -> { next(it.with(Header.X_URI_TEMPLATE of if (routeIdentity.isEmpty()) "/" else routeIdentity)) } }
                }
        }
    }

}