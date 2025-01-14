package org.http4k.mcp

import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Prompt.Get
import org.http4k.util.ObservableList

class Prompts(bindings: List<PromptBinding>) : ObservableList<PromptBinding>(bindings) {
    fun get(req: Get.Request): Get.Response = items
        .find { it.name == req.name }
        ?.let { Get.Response(it.toMessages(req.arguments)) }
        ?: error("no prompt")

    fun list(req: Prompt.List.Request) = Prompt.List.Response(items.map(PromptBinding::toPrompt))
}

