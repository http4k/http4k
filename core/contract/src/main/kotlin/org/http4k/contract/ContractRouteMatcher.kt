package org.http4k.contract

import org.http4k.contract.RouterMatch.MatchedWithoutHandler
import org.http4k.contract.RouterMatch.MatchingHandler
import org.http4k.contract.RouterMatch.MethodNotMatched
import org.http4k.contract.RouterMatch.Unmatched
import org.http4k.contract.security.NoSecurity.filter
import org.http4k.contract.security.Security
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.All
import org.http4k.routing.HttpMatchResult
import org.http4k.routing.Predicate
import org.http4k.routing.RouteMatcher
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.http4k.routing.RouterDescription
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
    private val predicate: Predicate = All,
) : RouteMatcher {
    private val contractRoot = PathSegments(rootAsString)

    private val notFound = preSecurityFilter
        .then(security?.filter ?: Filter.NoOp)
        .then(postSecurityFilter)
        .then { renderer.notFound() }

    override fun match(request: Request): HttpMatchResult {
        val m = internalMatch(request)
        return HttpMatchResult(
            m.priority,
            filter.then(
                when (m) {
                    is MatchingHandler -> m
                    is MatchedWithoutHandler -> notFound
                    is MethodNotMatched -> notFound
                    is Unmatched -> notFound
                }
            )
        )
    }

    private fun internalMatch(request: Request): RouterMatch {
        val unmatched: RouterMatch = Unmatched

        return if (request.isIn(contractRoot)) {
            routers.fold(unmatched) { memo, (routeFilter, router) ->
                when (memo) {
                    is MatchingHandler -> memo
                    else -> when (val matchResult = router.match(request)) {
                        is MatchingHandler -> MatchingHandler(routeFilter.then(matchResult))
                        else -> minOf(memo, matchResult)
                    }
                }
            }
        } else unmatched
    }

    override fun withBasePath(prefix: String) = copy(rootAsString = prefix + rootAsString)

    override fun withPredicate(other: Predicate): RouteMatcher = copy(predicate = predicate.and(other))

    override fun withFilter(new: Filter): RouteMatcher = copy(preSecurityFilter = new.then(preSecurityFilter))

    val description = RouterDescription(rootAsString,
        routes.map { it.toRouter(PathSegments("$it$descriptionPath")).description }
    )

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
