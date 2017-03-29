package com.gourame.http.routing

import com.gourame.http.core.HttpHandler
import com.gourame.http.core.Method
import com.gourame.http.core.Request
import com.gourame.http.core.Response
import com.gourame.http.core.Status
import com.gourame.http.core.Status.Companion.METHOD_NOT_ALLOWED
import com.gourame.http.core.Status.Companion.NOT_FOUND
import com.gourame.http.core.UriTemplate
import com.gourame.http.core.UriTemplate.Companion.uriTemplate

data class Route(val method: Method, val template: UriTemplate, val handler: HttpHandler)

infix fun Pair<Method, String>.by(action: HttpHandler): Route = Route(first, uriTemplate(second), action)

fun routes(vararg routes: Route): HttpHandler = RoutedHandler(*routes)

class RoutedHandler(vararg private val routes: Route) : HttpHandler {
    override fun invoke(request: Request): Response {
        val matchingResult = routes.toList().filter(
            matchesTemplates(request) otherwise NOT_FOUND.copy(description = "Route not found"),
            matchesMethod(request) otherwise METHOD_NOT_ALLOWED
        )
        return when (matchingResult) {
            is MatchingResult.NoMatch -> Response(matchingResult.status)
            is MatchingResult.Match -> matchingResult.candidates.first().let { it.handler(request.withUriTemplate(it.template)) }
        }
    }

    private fun RouteCandidates.filter(vararg criteria: Pair<RoutePredicate, Status>): MatchingResult<Status, RouteCandidates> {
        var remainingCandidates = this
        for (criterion in criteria) {
            remainingCandidates = remainingCandidates.filter(criterion.first)
            if (remainingCandidates.isEmpty()) return MatchingResult.NoMatch(criterion.second)
        }
        return MatchingResult.Match(remainingCandidates)
    }

    private fun matchesTemplates(request: Request): RoutePredicate = { (_, template) -> template.matches(request.uri.toString()) }
    private fun matchesMethod(request: Request): RoutePredicate = { (method, _) -> method == request.method }

    private infix fun RoutePredicate.otherwise(fallbackStatus: Status): Pair<RoutePredicate, Status> = Pair(this, fallbackStatus)
}

fun Request.withUriTemplate(uriTemplate: UriTemplate): Request = copy(headers = headers.plus("x-uri-template" to uriTemplate.toString()))
fun Request.pathParameter(name: String): String = this.uriTemplate().extract(uri.toString())[name] ?: throw IllegalArgumentException("path parameter '$name' not found")

private fun Request.uriTemplate(): UriTemplate = headers["x-uri-template"]?.let { UriTemplate.uriTemplate(it) } ?: throw RuntimeException("uri template not present in the request")

private typealias RoutePredicate = (Route) -> Boolean
private typealias RouteCandidates = List<Route>

private sealed class MatchingResult<out Status, out RouteCandidates> {
    class NoMatch<out Status>(val status: Status) : MatchingResult<Status, Nothing>()
    class Match<out RouteCandidates>(val candidates: RouteCandidates) : MatchingResult<Nothing, RouteCandidates>()
}