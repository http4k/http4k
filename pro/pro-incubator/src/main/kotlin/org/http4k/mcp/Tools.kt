package org.http4k.mcp

import org.http4k.connect.mcp.Tool
import org.http4k.util.ObservableList

class Tools(list: List<ToolBinding<*>>) : ObservableList<ToolBinding<*>>(list) {
    fun list(req: Tool.List.Request) =
        Tool.List.Response(items.map { it.toTool() })

    fun call(req: Tool.Call.Request): Tool.Call.Response =
        items
            .find { it.name == req.name }
            ?.call(req.arguments)
            ?: error("no tool")
}
