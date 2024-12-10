package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.then
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched

/**
 * For SPAs we serve static content as usual, or fall back to the index page. The resource loader is configured to look at
 * /public package (on the Classpath).
 */
fun singlePageApp(
    resourceLoader: ResourceLoader = ResourceLoader.Classpath("/public"),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
): RoutingHttpHandler =
    RoutingHttpHandler(
        listOf(SinglePageAppRouteMatcher("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap()))
    )

internal data class SinglePageAppRouteMatcher(
    private val pathSegments: String,
    private val resourceLoader: ResourceLoader,
    private val extraFileExtensionToContentTypes: Map<String, ContentType>,
    private val router: Router = All,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher<Response, Filter> {

    private val handler = ResourceLoadingHandler(pathSegments, resourceLoader, extraFileExtensionToContentTypes)

    override fun match(request: Request) = when (val m = router(request)) {
        is Matched -> {
            handler(request).let {
                when {
                    it.status != NOT_FOUND -> RoutingMatch(0, m.description, filter.then { _: Request -> it })
                    else -> handler(Request(GET, pathSegments)).let {
                        when {
                            it.status != NOT_FOUND -> RoutingMatch(0, m.description, filter.then { _: Request -> it })
                            else -> RoutingMatch(2, m.description,  filter.then { _: Request -> Response(NOT_FOUND) })
                        }
                    }
                }
            }
        }

        is NotMatched -> RoutingMatch(2, m.description, filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String): RouteMatcher<Response, Filter> = copy(pathSegments = prefix + pathSegments)
    override fun withFilter(new: Filter): RouteMatcher<Response, Filter> = copy(filter = new.then(filter))
    override fun withRouter(other: Router): RouteMatcher<Response, Filter> = copy(router = router.and(other))

    override fun toString() = "SPA at $pathSegments"

}
