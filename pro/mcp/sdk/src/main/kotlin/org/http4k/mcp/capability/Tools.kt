package org.http4k.mcp.capability

import org.http4k.core.Request
import org.http4k.mcp.protocol.McpTool
import org.http4k.mcp.util.ObservableList

/**
 * Handles protocol traffic for server provided tools.
 */
class Tools(list: List<ToolBinding>) : ObservableList<ToolBinding>(list) {

    fun list(req: McpTool.List.Request, http: Request) = McpTool.List.Response(items.map(ToolBinding::toTool))

    fun call(req: McpTool.Call.Request, http: Request) = items
        .find { it.toTool().name == req.name }
        ?.call(req, http)
        ?: error("no tool with name ${req.name}")
}
