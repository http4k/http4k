package org.http4k.contract

import org.http4k.contract.security.Security
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.Fallback
import org.http4k.routing.HttpMatchResult
import org.http4k.routing.Predicate
import org.http4k.routing.RouteMatcher
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.http4k.routing.RouterDescription
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched
import org.http4k.routing.and

data class ContractRouteMatcher(
    private val renderer: ContractRenderer,
    private val security: Security?,
    private val tags: Set<Tag>,
    private val descriptionSecurity: Security?,
    private val descriptionPath: String,
    private val preFlightExtraction: PreFlightExtraction,
    private val routes: List<ContractRoute> = emptyList(),
    private val rootAsString: String = "",
    private val preSecurityFilter: Filter = Filter.NoOp,
    private val postSecurityFilter: Filter = Filter.NoOp,
    private val includeDescriptionRoute: Boolean = false,
    private val webhooks: Map<String, List<WebCallback>> = emptyMap(),
    private val predicate: Predicate = Fallback,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher {
    private val contractRoot = PathSegments(rootAsString)

    override fun match(request: Request): HttpMatchResult {
        val m = internalMatch(request)
        return HttpMatchResult(
            m.priority,
            filter.then(
                when (m) {
                    is MatchingHandler -> m
                    is MatchedWithoutHandler -> { _: Request -> Response(Status.NOT_FOUND) }
                    is MethodNotMatched -> { _: Request -> Response(Status.METHOD_NOT_ALLOWED) }

                    is Unmatched -> { _: Request -> Response(Status.NOT_FOUND) }
                })
        )
    }

    private fun internalMatch(request: Request): RouterMatch {
        val unmatched: RouterMatch = Unmatched(description)

        return if (request.isIn(contractRoot)) {
            routers.fold(unmatched) { memo, (routeFilter, router) ->
                when (memo) {
                    is MatchingHandler -> memo
                    else -> when (val matchResult = router.match(request)) {
                        is MatchingHandler -> MatchingHandler(routeFilter.then(matchResult), description)
                        else -> minOf(memo, matchResult)
                    }
                }
            }
        } else unmatched
    }

    override fun withBasePath(prefix: String) = copy(rootAsString = prefix + rootAsString)

    override fun withPredicate(other: Predicate): RouteMatcher = copy(predicate = predicate.and(other))

    override fun withFilter(new: Filter): RouteMatcher = copy(filter = new.then(filter))

    val description = RouterDescription(rootAsString,
        routes.map { it.toRouter(PathSegments("$it$descriptionPath")).description }
    )

    private val notFound = preSecurityFilter
        .then(security?.filter ?: Filter.NoOp)
        .then(postSecurityFilter)
        .then { renderer.notFound() }

    private val handler: HttpHandler = {
        when (val matchResult = internalMatch(it)) {
            is MatchingHandler -> matchResult(it)
            is MethodNotMatched -> notFound(it)
            is Unmatched -> notFound(it)
            is MatchedWithoutHandler -> notFound(it)
        }
    }

    private val descriptionRoute =
        ContractRouteSpec0({ PathSegments("$it$descriptionPath") }, RouteMeta(operationId = "description"))
            .let {
                val extra = listOfNotNull(if (includeDescriptionRoute) it bindContract Method.GET to { _ ->
                    Response(
                        Status.OK
                    )
                } else null)
                it bindContract Method.GET to { _ ->
                    renderer.description(
                        contractRoot,
                        security,
                        (routes + extra).filter { route -> route.meta.described },
                        tags,
                        webhooks
                    )
                }
            }

    private val routers = routes
        .map {
            identify(it)
                .then(preSecurityFilter)
                .then(it.meta.security?.filter ?: security?.filter ?: Filter.NoOp)
                .then(postSecurityFilter)
                .then(ServerFilters.CatchLensFailure(renderer::badRequest))
                .then(PreFlightExtractionFilter(it.meta, preFlightExtraction)) to it.toRouter(contractRoot)
        } + (identify(descriptionRoute)
        .then(preSecurityFilter)
        .then(descriptionSecurity?.filter ?: Filter.NoOp)
        .then(postSecurityFilter) to descriptionRoute.toRouter(contractRoot))

    override fun toString() = contractRoot.toString() + "\n" + routes.joinToString("\n") { it.toString() }

    private fun identify(route: ContractRoute) =
        route.describeFor(contractRoot).let { routeIdentity ->
            Filter { next ->
                {
                    val xUriTemplate = UriTemplate.from(routeIdentity.ifEmpty { "/" })
                    RoutedResponse(next(RoutedRequest(it, xUriTemplate)), xUriTemplate)
                }
            }
        }
}
