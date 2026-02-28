package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.domain.ViewStore
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.Json.datastarModel

fun CreateView(viewStore: ViewStore) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/views" bind Method.POST to { req ->
            val signals = req.datastarModel<ViewSignals>()
            val name = signals.name?.ifEmpty { null }
            if (name != null) {
                viewStore.add(name, signals.normalizedFilter)
            }
            elements.renderViewBar(viewStore.list())
        }

    override fun mcp(): ToolCapability {
        val name = Tool.Arg.string().required("name", "Name for the new view")
        val direction = Tool.Arg.enum<Direction>().optional("direction", "Filter by direction: Inbound or Outbound")
        val method = Tool.Arg.string().optional("method", "Filter by HTTP method (GET, POST, etc)")
        val status = Tool.Arg.string().optional("status", "Filter by status code prefix (2, 4, 5, etc)")
        val path = Tool.Arg.string().optional("path", "Filter by path substring (case-insensitive)")
        val host = Tool.Arg.string().optional("host", "Filter by host substring (case-insensitive)")

        return Tool(
            "create_view",
            "Create a new transaction filter view",
            name, direction, method, status, path, host
        ) bind {
            viewStore.add(
                name(it),
                TransactionFilter(direction(it), host(it), method(it), status(it), path(it)).normalize()
            )
            Json.asToolResponse(viewStore.list())
        }
    }
}
