package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.long
import org.http4k.ai.mcp.model.status
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method
import org.http4k.core.Method.PUT
import org.http4k.lens.Path
import org.http4k.lens.long
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.domain.ViewStore
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.Json.datastarModel

fun UpdateView(viewStore: ViewStore) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/views/{id}" bind PUT to { req ->
            val id = Path.long().of("id")(req)
            val signals = req.datastarModel<ViewSignals>()
            viewStore.list().find { it.id == id }?.let { viewStore.update(it.copy(filter = signals.normalized)) }
            elements.renderViewBar(viewStore.list())
        }

    override fun mcp(): ToolCapability {
        val id = Tool.Arg.long().required("id", "ID of the view to update")
        val direction = Tool.Arg.enum<Direction>().optional("direction", "Filter by direction: Inbound or Outbound")
        val method = Tool.Arg.enum<Method>().optional("method", "Filter by HTTP method (GET, POST, etc)")
        val status = Tool.Arg.status().optional("status", "Filter by status code prefix (2, 4, 5, etc)")
        val path = Tool.Arg.string().optional("path", "Filter by path substring (case-insensitive)")
        val host = Tool.Arg.string().optional("host", "Filter by host substring (case-insensitive)")

        return Tool(
            "update_view",
            "Update an existing transaction filter view",
            id,
            direction,
            method,
            status,
            path,
            host
        ) bind { req ->
            viewStore.list().find { v -> v.id == id(req) }
                ?.let {
                    viewStore.update(
                        it.copy(
                            filter = TransactionFilter(
                                direction(req),
                                host(req),
                                method(req),
                                status(req),
                                path(req)
                            )
                        )
                    )
                }
            Json.asToolResponse(viewStore.list())
        }
    }
}
