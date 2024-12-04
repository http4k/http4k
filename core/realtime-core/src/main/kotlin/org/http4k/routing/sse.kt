package org.http4k.routing

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched
import org.http4k.sse.NoOp
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then

fun sse(sse: SseConsumer): SseHandler = { SseResponse(sse) }

fun sse(vararg methods: Pair<Method, SseHandler>): SseHandler = {
    methods.toMap()[it.method]?.invoke(it) ?: SseResponse { it.close() }
}

fun sse(vararg list: RoutingSseHandler): RoutingSseHandler = sse(list.toList())

fun sse(routers: List<RoutingSseHandler>) =
    RoutingSseHandler(routers.flatMap { it.routes })

class RoutedSseResponse(
    val delegate: SseResponse,
    override val xUriTemplate: UriTemplate,
) : SseResponse by delegate, RoutedMessage {
    override fun withConsumer(consumer: SseConsumer) =
        RoutedSseResponse(delegate.withConsumer(consumer), xUriTemplate)

    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()
}

class RoutingSseHandler(
    routes: List<RouteMatcher<SseResponse, SseFilter>>
) : RoutingHandler<SseResponse, SseFilter, RoutingSseHandler>(
    routes,
    SseResponse(NOT_FOUND, handled = false) { it.close() },
    ::RoutingSseHandler
)

data class SsePathMethod(val path: String, val method: Method) {
    infix fun to(handler: SseHandler) = when (handler) {
        is RoutingSseHandler -> handler.withRouter(method.asRouter()).withBasePath(path)
        else -> RoutingSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(path), handler, method.asRouter())))
    }
}

data class TemplatedSseRoute(
    private val uriTemplate: UriTemplate,
    private val handler: SseHandler,
    private val router: Router = All,
    private val filter: SseFilter = SseFilter.NoOp
): RouteMatcher<SseResponse, SseFilter> {
    init {
        require(handler !is RoutingSseHandler)
    }

    override fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = router(request)) {
            is Matched -> RoutingMatchResult(0, AddUriTemplate(uriTemplate).then(filter).then(handler))
            is NotMatched -> RoutingMatchResult(1, filter.then { _: Request -> SseResponse(result.status, handled = false) { it.close() } })
        }

        else -> RoutingMatchResult(2, filter.then { _: Request -> SseResponse(NOT_FOUND, handled = false) { it.close() } })
    }

    override fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    override fun withFilter(new: SseFilter) = copy(filter = new.then(filter))

    override fun withRouter(other: Router) = copy(router = router.and(other))

    override fun toString() = "template=$uriTemplate AND ${router.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = SseFilter { next ->
        {
            RoutedSseResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}
