package org.http4k.wiretap.otel

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.html
import org.http4k.lens.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json

fun Index(templates: TemplateRenderer): RoutingHttpHandler =
    "/" bind GET to { req ->
        val trace = traceParam(req)
        Response(OK).html(templates(Index(trace)))
    }

private val traceParam = Query.string().optional("trace")

data class OtelSignals(
    val selectedTrace: String? = null
)

data class Index(val deepLinkTrace: String? = null) : ViewModel {
    val initialSignals = Json.asDatastarSignals(OtelSignals(selectedTrace = deepLinkTrace))
}
