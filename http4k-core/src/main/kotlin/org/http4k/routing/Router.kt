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

    val description: RouterDescription get() = RouterDescription.unavailable
}

data class RouterDescription(val description: String, val children: List<RouterDescription> = listOf()) {
    companion object {
        val unavailable = RouterDescription("unavailable")
    }
}

/**
 * The result of a matching operation. May or may not contain a matched HttpHandler.
 */
sealed class RouterMatch(private val priority: Int, open val description: RouterDescription, open val subMatches: List<RouterMatch>) : Comparable<RouterMatch> {
    data class MatchingHandler(private val httpHandler: HttpHandler, override val description: RouterDescription, override val subMatches: List<RouterMatch> = listOf()) : RouterMatch(0, description, subMatches), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
        override fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch = copy(description = description, subMatches = fromMatches)
    }

    data class MatchedWithoutHandler(override val description: RouterDescription, override val subMatches: List<RouterMatch> = listOf()) : RouterMatch(1, description, subMatches) {
        override fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch = copy(description = description, subMatches = fromMatches)
    }

    data class MethodNotMatched(override val description: RouterDescription, override val subMatches: List<RouterMatch> = listOf()) : RouterMatch(2, description, subMatches) {
        override fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch = copy(description = description, subMatches = fromMatches)
    }

    data class Unmatched(override val description: RouterDescription, override val subMatches: List<RouterMatch> = listOf()) : RouterMatch(3, description, subMatches) {
        override fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch = copy(description = description, subMatches = fromMatches)
    }

    override fun compareTo(other: RouterMatch): Int = priority.compareTo(other.priority)
    abstract fun aggregatedBy(description: RouterDescription, fromMatches: List<RouterMatch>): RouterMatch
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
    override fun match(request: Request): RouterMatch {
        val matches = list.map { next -> next.match(request) }
        val result = matches.minOrNull() ?: Unmatched(description)
        return result.aggregatedBy(description, matches)
    }

    override fun withBasePath(new: String) = from(list.map { it.withBasePath(new) })

    override fun withFilter(new: Filter) = from(list.map { it.withFilter(new) })

    override val description: RouterDescription
        get() =
            RouterDescription("or", list.map { it.description })

    companion object {
        fun from(list: List<Router>): Router = if (list.size == 1) list.first() else OrRouter(list)
    }
}

internal data class AndRouter private constructor(private val list: List<Router>) : Router {
    override fun match(request: Request): RouterMatch {
        val matches = list.map { it.match(request) }
        val result = matches.reduce(RouterMatch::and)
        return result.aggregatedBy(description, matches)
    }

    override fun withBasePath(new: String) = from(list.map { it.withBasePath(new) })

    override fun withFilter(new: Filter) = from(list.map { it.withFilter(new) })

    override val description = RouterDescription("and", list.map { it.description })

    companion object {
        fun from(list: List<Router>): Router = if (list.size == 1) list.first() else AndRouter(list)
    }
}

internal data class PassthroughRouter(private val handler: HttpHandler) : Router {
    override fun match(request: Request): RouterMatch = MatchingHandler(handler, description)

    override fun withBasePath(new: String) = when (handler) {
        is RoutingHttpHandler -> handler.withBasePath(new)
        else -> TemplateRouter(UriTemplate.from(new), handler)
    }

    override fun withFilter(new: Filter) = when (handler) {
        is RoutingHttpHandler -> handler.withFilter(new)
        else -> PassthroughRouter(new.then(handler))
    }

    override val description = RouterDescription("<http-handler>")
}

internal data class Prefix(private val template: String) : Router {
    override fun match(request: Request) = when {
        UriTemplate.from("$template{match:.*}").matches(request.uri.path) -> MatchedWithoutHandler(description)
        else -> Unmatched(description)
    }

    override fun withBasePath(new: String) = Prefix("$new/${template.trimStart('/')}")

    override val description = RouterDescription("prefix == '$template'")
}

internal data class TemplateRouter(private val template: UriTemplate,
                                   private val httpHandler: HttpHandler) : Router {
    override fun match(request: Request) = when {
        template.matches(request.uri.path) ->
            MatchingHandler({ RoutedResponse(httpHandler(RoutedRequest(it, template)), template) }, description)
        else -> Unmatched(description)
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

    override val description = RouterDescription("template == '$template'")
}
