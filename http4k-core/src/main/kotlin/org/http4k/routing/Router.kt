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
interface Router {
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

    fun getDescription(): RouterDescription = RouterDescription.unavailable
}

data class RouterDescription(val description: String, val children: List<RouterDescription> = listOf()) {
    companion object {
        val unavailable = RouterDescription("unavailable")
    }
}

/**
 * The result of a matching operation. May or may not contain a matched HttpHandler.
 */
sealed class RouterMatch(private val priority: Int, open val description: RouterDescription) : Comparable<RouterMatch> {
    data class MatchingHandler(private val httpHandler: HttpHandler, override val description: RouterDescription) : RouterMatch(0, description), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
    }

    class MatchedWithoutHandler(description: RouterDescription) : RouterMatch(1, description)
    class MethodNotMatched(description: RouterDescription) : RouterMatch(2, description)
    class Unmatched(description: RouterDescription) : RouterMatch(3, description)

    override fun compareTo(other: RouterMatch): Int = priority.compareTo(other.priority)
}

internal fun RouterMatch.and(other: RouterMatch): RouterMatch = when (this) {
    is MatchedWithoutHandler -> other
    is MethodNotMatched -> when (other) {
        is MatchingHandler, is MatchedWithoutHandler, is MethodNotMatched -> this
        is Unmatched -> other
    }
    is MatchingHandler, is Unmatched -> this
}

internal data class OrRouter private constructor(private val list: List<Router>) : Router {
    override fun match(request: Request) = list
        .map { next -> next.match(request) }
        .minOrNull() ?: Unmatched(getDescription())

    override fun withBasePath(new: String) = from(list.map { it.withBasePath(new) })

    override fun withFilter(new: Filter) = from(list.map { it.withFilter(new) })

    override fun getDescription(): RouterDescription =
        RouterDescription("or", list.map { it.getDescription() })

    companion object {
        fun from(list: List<Router>): Router = if (list.size == 1) list.first() else OrRouter(list)
    }
}

internal data class AndRouter private constructor(private val list: List<Router>) : Router {
    override fun match(request: Request) =
        list.fold(MatchedWithoutHandler(getDescription()) as RouterMatch) { acc, next -> acc.and(next.match(request)) }

    override fun withBasePath(new: String) = from(list.map { it.withBasePath(new) })

    override fun withFilter(new: Filter) = from(list.map { it.withFilter(new) })

    override fun getDescription() = RouterDescription("and", list.map { it.getDescription() })

    companion object {
        fun from(list: List<Router>): Router = if (list.size == 1) list.first() else AndRouter(list)
    }
}

internal data class PassthroughRouter(private val handler: HttpHandler) : Router {
    override fun match(request: Request): RouterMatch = MatchingHandler(handler, getDescription())

    override fun withBasePath(new: String) = when (handler) {
        is RoutingHttpHandler -> handler.withBasePath(new)
        else -> TemplateRouter(UriTemplate.from(new), handler)
    }

    override fun withFilter(new: Filter) = when (handler) {
        is RoutingHttpHandler -> handler.withFilter(new)
        else -> PassthroughRouter(new.then(handler))
    }

    override fun getDescription() = RouterDescription("<http-handler>")
}

internal data class Prefix(private val template: String) : Router {
    override fun match(request: Request) = when {
        UriTemplate.from("$template{match:.*}").matches(request.uri.path) -> MatchedWithoutHandler(getDescription())
        else -> Unmatched(getDescription())
    }

    override fun withBasePath(new: String) = Prefix("$new/${template.trimStart('/')}")
    override fun getDescription() = RouterDescription("prefix == '$template'")
}

internal data class TemplateRouter(private val template: UriTemplate,
                                   private val httpHandler: HttpHandler) : Router {
    override fun match(request: Request) = when {
        template.matches(request.uri.path) ->
            MatchingHandler({ RoutedResponse(httpHandler(RoutedRequest(it, template)), template) }, getDescription())
        else -> Unmatched(getDescription())
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

    override fun getDescription() = RouterDescription("template == '$template'")
}
