package org.http4k.wiretap.traffic

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

fun Index(templates: TemplateRenderer, findByTraceId: (String) -> Long?): RoutingHttpHandler =
    "/" bind GET to { req ->
        Response(OK).html(templates(Index(traceParam(req)?.let { findByTraceId(it) })))
    }

private val traceParam = Query.string().optional("trace")

data class TrafficSignals(
    val direction: String? = null,
    val host: String? = null,
    val method: String? = null,
    val status: String? = null,
    val path: String? = null,
    val activeView: String = "all",
    val customView: Boolean = false,
    val showAddView: Boolean = false,
    val name: String? = null,
    val selectedTx: Long? = null
)

data class Index(val deepLinkTxId: Long? = null) : ViewModel {
    val initialSignals = Json.asDatastarSignals(TrafficSignals(selectedTx = deepLinkTxId))
}
