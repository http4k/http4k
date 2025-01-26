package org.http4k.mcp.capability

import org.http4k.core.Request
import org.http4k.mcp.protocol.McpPrompt
import org.http4k.mcp.protocol.McpPrompt.Get
import org.http4k.mcp.util.ObservableList

/**
 * Handles protocol traffic for prompts features.
 */
class Prompts(bindings: List<PromptBinding>) : ObservableList<PromptBinding>(bindings) {
    fun get(req: Get.Request, http: Request) = items
        .find { it.toPrompt().name == req.name }
        ?.get(req, http)
        ?: error("no prompt")

    fun list(mcp: McpPrompt.List.Request, http: Request) =
        McpPrompt.List.Response(items.map(PromptBinding::toPrompt))
}

