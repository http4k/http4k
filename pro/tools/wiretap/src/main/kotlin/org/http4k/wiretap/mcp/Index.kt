package org.http4k.wiretap.mcp

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json

data class McpClientSignals(
    val activeTab: String = "tools",
    val selectedItem: String = "",
    val selectedServerId: String = "",
    val iframeVisible: Boolean = false
)

data class Index(val dummy: Boolean = true) : ViewModel {
    val initialSignals: String = Json.asFormatString(McpClientSignals())
}

fun McpIndex(html: TemplateRenderer) = "/" bind GET to {
    Response(OK).html(html(Index()))
}
