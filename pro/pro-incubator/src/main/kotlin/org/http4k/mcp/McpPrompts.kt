package org.http4k.mcp

import org.http4k.connect.mcp.McpPrompt
import org.http4k.connect.mcp.McpPrompt.Get
import org.http4k.core.Request
import org.http4k.routing.RoutedPrompt
import org.http4k.util.ObservableList

class McpPrompts(bindings: List<RoutedPrompt>) : ObservableList<RoutedPrompt>(bindings) {
    fun get(req: Get.Request, http: Request) = items
        .find { it.prompt.name == req.name }
        ?.let { it(req.arguments, http) }
        ?: error("no prompt")

    fun list(req: McpPrompt.List.Request) = McpPrompt.List.Response(items.map(RoutedPrompt::toPrompt))
}

