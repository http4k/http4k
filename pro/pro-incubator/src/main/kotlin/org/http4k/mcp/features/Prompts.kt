package org.http4k.mcp.features

import org.http4k.core.Request
import org.http4k.mcp.protocol.McpPrompt
import org.http4k.mcp.protocol.McpPrompt.Get
import org.http4k.routing.PromptFeatureBinding
import org.http4k.util.ObservableList

/**
 * Handles protocol traffic for prompts features.
 */
class Prompts(bindings: List<PromptFeatureBinding>) : ObservableList<PromptFeatureBinding>(bindings), McpFeature {
    fun get(req: Get.Request, http: Request) = items
        .find { it.prompt.name == req.name }
        ?.get(req.arguments, http)
        ?: error("no prompt")

    fun list(req: McpPrompt.List.Request, http: Request) =
        McpPrompt.List.Response(items.map(PromptFeatureBinding::toPrompt))
}

