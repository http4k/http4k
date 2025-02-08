package client

import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.client.SseMcpClient
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity

fun main() {
    val mcpClient = SseMcpClient(
        Request(GET, "http://localhost:3001/sse"),
        VersionedMcpEntity(McpEntity.of("foobar"), Version.of("1.0.0")),
        ClientCapabilities(),
        JavaHttpClient(responseBodyMode = Stream)
    )

    println(mcpClient.start())

    println(mcpClient.prompts().list().getOrThrow())
    println(mcpClient.prompts().get("prompt2", PromptRequest(mapOf("a1" to "foo"))).getOrThrow())

    println(mcpClient.resources().list().getOrThrow())
    println(mcpClient.resources().read(ResourceRequest(Uri.of("https://www.http4k.org"))).getOrThrow())

    println(
        mcpClient.completions()
            .complete(CompletionRequest(Reference.Prompt("prompt2"), CompletionArgument("foo", "bar"))).getOrThrow()
    )

    println(
        mcpClient.sampling().sample(
            ModelIdentifier.of("asd"),
            SamplingRequest(listOfNotNull(), MaxTokens.of(123))
        ).map { it.getOrThrow() }.toList()
    )

    println(mcpClient.tools().list().getOrThrow())
    println(mcpClient.tools().call(ToolName.of("weather"), ToolRequest(mapOf("city" to "london"))).getOrThrow())

    mcpClient.stop()
}
