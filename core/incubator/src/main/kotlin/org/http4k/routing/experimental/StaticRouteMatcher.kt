package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri.Companion.of
import org.http4k.core.then
import org.http4k.routing.All
import org.http4k.routing.RouteMatcher
import org.http4k.routing.Router
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.RoutingMatch
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched
import org.http4k.routing.and

fun static(
    resourceLoader: RouteMatcher<Response, Filter>,
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
) = RoutingHttpHandler(
    listOf(StaticRouteMatcher("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap()))
)

data class StaticRouteMatcher(
    private val pathSegments: String,
    private val resourceLoader: RouteMatcher<Response, Filter>,
    private val extraFileExtensionToContentTypes: Map<String, ContentType>,
    private val router: Router = All,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher<Response, Filter>{
    override fun match(request: Request) = when (val result = router(request)) {
        is Matched -> resourceLoader.match(request.uri(of(convertPath(request.uri.path))))
        is NotMatched -> RoutingMatch(2, result.description, filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String): RouteMatcher<Response, Filter> = copy(pathSegments = prefix + pathSegments)
    override fun withFilter(new: Filter): RouteMatcher<Response, Filter> = copy(filter = new.then(filter))
    override fun withRouter(other: Router): RouteMatcher<Response, Filter> = copy(router = router.and(other))

    private fun convertPath(path: String) =
        if (pathSegments == "/" || pathSegments == "") path else path.replace(pathSegments, "")

    override fun toString() = "Static files $pathSegments"
}
