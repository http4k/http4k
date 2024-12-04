package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched

/**
 * Applies the generic templating routing logic regardless of protocol
 */
abstract class TemplatedRoute<R, F : ((Request) -> R) -> (Request) -> R, Self : RouteMatcher<R, F>>(
    protected val uriTemplate: UriTemplate,
    protected val handler: (Request) -> R,
    protected val router: Router,
    protected val filter: F,
    private val responseFor: (Status) -> R,
    private val addUriTemplateFilter: ((Request) -> R) -> (Request) -> R
) : RouteMatcher<R, F> {

    init {
        require(handler !is RoutingHandler<*, *, *>)
    }

    override fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = router(request)) {
            is Matched -> RoutingMatchResult(0, addUriTemplateFilter(filter(handler)))
            is NotMatched -> RoutingMatchResult(1, filter { responseFor(result.status) })
        }

        else -> RoutingMatchResult(2, filter { _: Request -> responseFor(NOT_FOUND) })
    }

    override fun toString() = "template=$uriTemplate AND ${router.description}"
}

