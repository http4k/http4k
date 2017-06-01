package org.http4k.contract

import org.http4k.contract.PathBinder.Companion.Core
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.http4k.routing.Router

data class ContractRouter internal constructor(private val rootAsString: String,
                                              private val renderer: ContractRenderer,
                                              private val filter: Filter,
                                              private val security: Security,
                                              private val descriptionPath: String,
                                              private val routes: List<ServerRoute>) : Router {
    private val contractRoot = BasePath(rootAsString)
    fun withDescriptionPath(path: String) = copy(descriptionPath = path)
    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = copy(routes = routes + new)
    fun securedBy(new: Security) = copy(security = new)

    private val descriptionRoute = PathBinder0(Core(Route("description route"), GET, { BasePath("$it$descriptionPath") })) bind
        { renderer.description(contractRoot, security, routes) }

    private val routers: List<Pair<Router, Filter>> = routes
        .map { it.router(contractRoot) to security.filter.then(identify(it)).then(filter) }
        .plus(descriptionRoute.router(contractRoot) to identify(descriptionRoute).then(filter))

    private val noMatch: HttpHandler? = null

    override fun match(request: Request): HttpHandler? =
        if (request.isIn(contractRoot)) {
            routers.fold(noMatch, { memo, (router, routeFilter) ->
                memo ?: router.match(request)?.let { routeFilter.then(it) }
            })
        } else null

    private fun identify(route: ServerRoute): Filter {
        val routeIdentity = route.describeFor(contractRoot)
        return Filter {
            { req ->
                it(req.with(X_URI_TEMPLATE of if (routeIdentity.isEmpty()) "/" else routeIdentity))
            }
        }
    }
}