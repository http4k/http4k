package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched

/**
 * Matches requests for routing purposes.
 */
fun interface Router {
    /**
     * Attempt to supply an HttpHandler which can service the passed request.
     */
    fun match(request: Request): RouterMatch

    /**
     * Returns a Router which prepends the passed base path to the logic determining the match().
     */
    fun withBasePath(new: String): Router = Prefix(new).and(this)

    /**
     * Returns a Router which applies the passed Filter to all received requests before servicing them.
     */
    fun withFilter(new: Filter): Router = this
}

/**
 * The result of a matching operation. May or may not contain a matched HttpHandler.
 */
sealed class RouterMatch(private val priority: Int) : Comparable<RouterMatch> {
    data class MatchingHandler(private val httpHandler: HttpHandler) : RouterMatch(0), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
    }

    object MatchedWithoutHandler : RouterMatch(1)
    object MethodNotMatched : RouterMatch(2)
    object Unmatched : RouterMatch(3)

    override fun compareTo(other: RouterMatch): Int = priority.compareTo(other.priority)
}

internal fun RouterMatch.and(other: RouterMatch): RouterMatch = when (this) {
    is MatchedWithoutHandler -> other
    is MatchingHandler, MethodNotMatched, Unmatched -> this
}

internal data class OrRouter private constructor(private val list: List<Router>) : Router {
    override fun match(request: Request) = list.asSequence()
        .map { next -> next.match(request) }
        .sorted()
        .firstOrNull() ?: Unmatched

    override fun withBasePath(new: String) = from(list.map { it.withBasePath(new) })

    override fun withFilter(new: Filter) = from(list.map { it.withFilter(new) })

    companion object {
        fun from(list: List<Router>): Router = if (list.size == 1) list.first() else OrRouter(list)
    }
}

internal data class AndRouter private constructor(private val list: List<Router>) : Router {
    override fun match(request: Request) =
        list.fold(MatchedWithoutHandler as RouterMatch) { acc, next -> acc.and(next.match(request)) }

    override fun withBasePath(new: String) = from(list.map { it.withBasePath(new) })

    override fun withFilter(new: Filter) = from(list.map { it.withFilter(new) })

    companion object {
        fun from(list: List<Router>): Router = if (list.size == 1) list.first() else AndRouter(list)
    }
}

internal data class PassthroughRouter(private val handler: HttpHandler) : Router {
    override fun match(request: Request): RouterMatch = MatchingHandler(handler)

    override fun withBasePath(new: String) = when (handler) {
        is RoutingHttpHandler -> handler.withBasePath(new)
        else -> TemplateRouter(UriTemplate.from(new), handler)
    }

    override fun withFilter(new: Filter) = when (handler) {
        is RoutingHttpHandler -> handler.withFilter(new)
        else -> PassthroughRouter(new.then(handler))
    }
}

internal data class Prefix(private val template: String) : Router {
    override fun match(request: Request) = when {
        UriTemplate.from("$template{match:.*}").matches(request.uri.path) -> MatchedWithoutHandler
        else -> Unmatched
    }

    override fun withBasePath(new: String) = Prefix("$new/${template.trimStart('/')}")
}

internal data class TemplateRouter(private val template: UriTemplate,
                                   private val httpHandler: HttpHandler) : Router {
    override fun match(request: Request) = when {
        template.matches(request.uri.path) ->
            MatchingHandler { RoutedResponse(httpHandler(RoutedRequest(it, template)), template) }
        else -> Unmatched
    }

    override fun withBasePath(new: String): Router =
        TemplateRouter(UriTemplate.from("$new/${template}"),
            when (httpHandler) {
                is RoutingHttpHandler -> httpHandler.withBasePath(new)
                else -> httpHandler
            })

    override fun withFilter(new: Filter): Router = copy(httpHandler = when (httpHandler) {
        is RoutingHttpHandler -> httpHandler.withFilter(new)
        else -> new.then(httpHandler)
    })
}
