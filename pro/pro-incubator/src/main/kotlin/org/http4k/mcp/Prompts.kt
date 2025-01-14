package org.http4k.mcp

import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Prompt.Get

class Prompts(bindings: List<PromptBinding>) {
    private val prompts = bindings.toMutableList()

    fun add(binding: PromptBinding) {
        prompts += binding
    }

    fun get(req: Get.Request): Get.Response = prompts
        .find { it.name == req.name }
        ?.let { Get.Response(it.toMessages(req.arguments)) }
        ?: error("no prompt")

    fun list(req: Prompt.List.Request) = Prompt.List.Response(
        prompts.map(PromptBinding::toPrompt)
    )
}

