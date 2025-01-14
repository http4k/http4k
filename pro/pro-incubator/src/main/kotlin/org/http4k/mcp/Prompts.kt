package org.http4k.mcp

import org.http4k.connect.mcp.Prompt

class Prompts(private val bindings: List<PromptBinding>) {
    fun get(req: Prompt.Get.Request): Prompt.Get.Response {

        TODO()
    }

    fun list(req: Prompt.List.Request) =
        Prompt.List.Response(
            listOf(
                Prompt("1", "2", null),
                Prompt("12", "222", listOf(
                    Prompt.Argument("p1", "d1", true),
                    Prompt.Argument("p2", "d2", false)
                )),
            )
        )
}

