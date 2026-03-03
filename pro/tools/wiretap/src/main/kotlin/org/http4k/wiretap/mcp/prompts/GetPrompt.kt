package org.http4k.wiretap.mcp.prompts

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.PromptName
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.core.Body
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.SignalModel

data class GetPromptSignals(val promptName: String = "", val promptArgs: String = "{}") : SignalModel

data class PromptMessageView(val role: String, val content: String)

data class PromptResultView(val messages: List<PromptMessageView>) : ViewModel

private val getPromptSignalsLens = with(Json) { Body.auto<GetPromptSignals>().toLens() }

fun GetPrompt(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/prompts/get" bind POST to { req ->
        val signals = getPromptSignalsLens(req)
        val name = PromptName.of(signals.promptName)

        @Suppress("UNCHECKED_CAST")
        val arguments = runCatching {
            Json.asA<Map<String, String>>(signals.promptArgs)
        }.getOrDefault(emptyMap())

        val result = mcpClient.prompts().get(name, PromptRequest(arguments))
        val view = result.map { response ->
            PromptResultView(response.messages.map { msg ->
                PromptMessageView(msg.role.value, msg.content.toString())
            })
        }.valueOrNull() ?: PromptResultView(listOf(PromptMessageView("error", "Prompt get failed")))

        Response(OK).datastarElements(
            elements(view),
            selector = Selector.of("#prompt-result")
        )
    }
