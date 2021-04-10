package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.Unmatched

/**
 * For SPAs we serve static content as usual, or fall back to the index page. The resource loader is configured to look at
 * /public package (on the Classpath).
 */
fun singlePageApp(resourceLoader: ResourceLoader = ResourceLoader.Classpath("/public"), vararg extraFileExtensionToContentTypes: Pair<String, ContentType>): RoutingHttpHandler =
    SinglePageAppRoutingHandler("", StaticRoutingHttpHandler("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap()))

internal data class SinglePageAppRoutingHandler(
    private val pathSegments: String,
    private val staticHandler: StaticRoutingHttpHandler
) : RoutingHttpHandler {

    override fun invoke(request: Request): Response {
        val matchOnStatic = when (val matchResult = staticHandler.match(request)) {
            is MatchingHandler -> matchResult(request)
            else -> null
        }

        val matchOnIndex = when (val matchResult = staticHandler.match(Request(GET, pathSegments))) {
            is MatchingHandler -> matchResult
            else -> null
        }

        val fallbackHandler = matchOnIndex ?: { Response(NOT_FOUND) }
        return matchOnStatic ?: fallbackHandler(Request(GET, pathSegments))
    }

    override fun match(request: Request) = if(request.method == GET) {
        MatchingHandler(this, description)
    } else Unmatched(RouterDescription("template == '$pathSegments'"))

    override fun withFilter(new: Filter) = copy(staticHandler = staticHandler.withFilter(new) as StaticRoutingHttpHandler)

    override fun withBasePath(new: String) = SinglePageAppRoutingHandler(new + pathSegments, staticHandler.withBasePath(new) as StaticRoutingHttpHandler)
}
