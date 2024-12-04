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
) : RouteMatcher {

    private val handler = ResourceLoadingHandler(pathSegments, resourceLoader, extraFileExtensionToContentTypes)

    override fun match(request: Request): HttpMatchResult = when (router(request)) {
        is Matched -> {
            handler(request).let {
                when {
                    it.status != NOT_FOUND -> HttpMatchResult(0, filter.then { _: Request -> it })
                    else -> handler(Request(GET, pathSegments)).let {
                        when {
                            it.status != NOT_FOUND -> HttpMatchResult(0, filter.then { _: Request -> it })
                            else -> HttpMatchResult(2, filter.then { _: Request -> Response(NOT_FOUND) })
                        }
                    }
                }
            }
        }

        is NotMatched -> HttpMatchResult(2, filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String): RouteMatcher = copy(pathSegments = prefix + pathSegments)
    override fun withFilter(new: Filter): RouteMatcher = copy(filter = new.then(filter))
    override fun withRouter(other: Router): RouteMatcher = copy(router = router.and(other))

    override fun toString() = "SPA at $pathSegments"

}
