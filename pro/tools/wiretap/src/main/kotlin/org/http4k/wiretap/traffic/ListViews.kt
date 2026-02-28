package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.model.Tool
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.View
import org.http4k.wiretap.util.Json

fun ListViews(list: () -> List<View>) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/views" bind Method.GET to { elements.renderViewBar(list()) }

    override fun mcp() = Tool(
        "list_views",
        "List all transaction filter views"
    ) bind {
        Json.asToolResponse(list())
    }
}
