package org.http4k.connect.mcp

import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.SampleResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.ModelName
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.McpEntity
import org.http4k.mcp.server.ServerMetaData
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val mcpServer = mcp(
        ServerMetaData(McpEntity("http4k mcp server", Version.of("0.1.0"))),
        prompt1(),
        prompt2(),
        staticResource(),
        templatedResource(),
        reverseTool(),
        countingTool(),
        llm()
    )

    mcpServer.debug(debugStream = true).asServer(Helidon(3001)).start()
}

private fun llm() = ModelSelector(ModelName.of("my model")) { 1 } bind {
    SampleResponse(ModelName.of("my model"), StopReason.of("stop"), Role.assistant, Content.Text("content"))
}

private fun countingTool() = Tool("count", "description", Multiply(1, 2)) bind {
    ToolResponse.Ok(listOf(Content.Text(it.input.first + it.input.second)))
}

private fun reverseTool() = Tool("reverse", "description", Reverse("name")) bind {
    ToolResponse.Ok(listOf(Content.Text(it.input.input.reversed())))
}

private fun templatedResource() = Resource.Templated(
    Uri.of("https://www.http4k.org/ecosystem/{+ecosystem}/"),
    "HTTP4K ecosystem page",
    "view ecosystem"
) bind LinksOnPage(JavaHttpClient())

private fun staticResource() =
    Resource.Static(Uri.of("https://www.http4k.org"), "HTTP4K", "description") bind LinksOnPage(JavaHttpClient())

private fun prompt1() = Prompt("prompt1", "description1") bind {
    PromptResponse("description", listOf(Message(Role.assistant, Content.Text(it.input.toString()))))
}

private fun prompt2() = Prompt("prompt2", "description1") bind {
    PromptResponse("description", listOf(Message(Role.assistant, Content.Text(it.input.toString()))))
}

data class Reverse(val input: String)
data class Multiply(val first: Int, val second: Int)
