package org.http4k.contract

import org.http4k.contract.security.Security
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.LensFailure
import org.http4k.lens.Validator
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.http4k.routing.RouterDescription
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched
import org.http4k.routing.RoutingHttpHandler

data class ContractRoutingHttpHandler(
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
    private val webhooks: Map<String, List<WebCallback>> = emptyMap()
) : RoutingHttpHandler {
    private val contractRoot = PathSegments(rootAsString)

    fun withPostSecurityFilter(new: Filter) = copy(postSecurityFilter = postSecurityFilter.then(new))

    /**
     * NOTE: By default, filters for Contracts are applied *before* the Security filter. Use withPostSecurityFilter()
     * to achieve population of filters after security.
     */
    override fun withFilter(new: Filter) = copy(preSecurityFilter = new.then(preSecurityFilter))

    override fun withBasePath(new: String) = copy(rootAsString = new + rootAsString)

    override val description = RouterDescription(rootAsString,
        routes.map { it.toRouter(PathSegments("$it$descriptionPath")).description }
    )

    private val notFound = preSecurityFilter.then(
        security?.filter
            ?: Filter.NoOp
    ).then(postSecurityFilter).then { renderer.notFound() }

    private val handler: HttpHandler = {
        when (val matchResult = match(it)) {
            is MatchingHandler -> matchResult(it)
            is MethodNotMatched -> notFound(it)
            is Unmatched -> notFound(it)
            is MatchedWithoutHandler -> notFound(it)
        }
    }

    override fun invoke(request: Request): Response = handler(request)

    private val descriptionRoute =
        ContractRouteSpec0({ PathSegments("$it$descriptionPath") }, RouteMeta(operationId = "description"))
            .let {
                val extra =
                    listOfNotNull(if (includeDescriptionRoute) it bindContract GET to { _ -> Response(OK) } else null)
                it bindContract GET to { _ ->
                    renderer.description(
                        contractRoot,
                        security,
                        routes + extra,
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
                .then(CatchLensFailure(renderer::badRequest))
                .then(PreFlightExtractionFilter(it.meta, preFlightExtraction)) to it.toRouter(contractRoot)
        } + (identify(descriptionRoute)
        .then(preSecurityFilter)
        .then(descriptionSecurity?.filter ?: Filter.NoOp)
        .then(postSecurityFilter) to descriptionRoute.toRouter(contractRoot))

    override fun toString() = contractRoot.toString() + "\n" + routes.joinToString("\n") { it.toString() }

    override fun match(request: Request): RouterMatch {
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

internal class PreFlightExtractionFilter(meta: RouteMeta, preFlightExtraction: PreFlightExtraction) : Filter {
    private val preFlightChecks = (meta.preFlightExtraction ?: preFlightExtraction)(meta)
    override fun invoke(next: HttpHandler): HttpHandler = {
        when (it.method) {
            OPTIONS -> next(it)
            else -> {
                val failures = Validator.Strict(it, preFlightChecks)
                if (failures.isEmpty()) next(it) else throw LensFailure(failures, target = it)
            }
        }
    }
}
