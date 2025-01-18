package org.http4k.mcp.server

import org.http4k.core.Request
import org.http4k.mcp.protocol.McpTool
import org.http4k.routing.RoutedTool
import org.http4k.util.ObservableList

class McpTools(list: List<RoutedTool<*>>) : ObservableList<RoutedTool<*>>(list) {

    fun list(req: McpTool.List.Request) = McpTool.List.Response(items.map { it.toTool() })

    fun call(req: McpTool.Call.Request, http: Request) =
        items
            .find { it.toTool().name == req.name }
            ?.invoke(req.arguments, http)
            ?: error("no tool")
}
