package org.http4k.wiretap.mcp.tools

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.model.ToolName
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

data class CallToolSignals(val toolName: String = "", val toolArgs: String = "{}") : SignalModel

data class ToolResultView(val result: String) : ViewModel

private val callToolSignalsLens = with(Json) { Body.auto<CallToolSignals>().toLens() }

fun CallTool(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/call" bind POST to { req ->
        val signals = callToolSignalsLens(req)
        val name = ToolName.of(signals.toolName)

        @Suppress("UNCHECKED_CAST")
        val arguments = runCatching { Json.asA<Map<String, Any>>(signals.toolArgs) }.getOrDefault(emptyMap())

        val result = mcpClient.tools().call(name, ToolRequest(arguments))
        val view = result.map { ToolResultView(Json.asFormatString(it)) }
            .valueOrNull() ?: ToolResultView("""{"error":"Tool call failed"}""")

        Response(OK).datastarElements(
            elements(view),
            selector = Selector.of("#tool-result")
        )
    }
