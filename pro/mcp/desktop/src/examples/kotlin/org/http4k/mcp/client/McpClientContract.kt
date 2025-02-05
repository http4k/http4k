package org.http4k.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.Tool
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.bind
import org.http4k.routing.mcpSse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

interface McpClientContract : PortBasedTest {
    @Test
    fun `can interact with server`() {
        val server = mcpSse(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            Prompt("prompt", "description1") bind {
                PromptResponse(listOf(Message(Role.assistant, Content.Text(it.toString()))), "description")
            },
            Tool("reverse", "description", Tool.Arg.required("name")) bind {
                ToolResponse.Ok(listOf(Content.Text(it.javaClass.simpleName.toString().reversed())))
            },
            Resource.Static(Uri.of("https://http4k.org"), "HTTP4K", "description") bind {
                ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of(""))))
            },
            Reference.Resource(Uri.of("https://http4k.org")) bind {
                CompletionResponse(Completion(listOf("1", "2")))
            },
        )
            .asServer(Helidon(0)).start()

        val mcpClient = clientFor(Uri.of("http://localhost:${server.port()}/sse"))

        mcpClient.start()

        assertThat(mcpClient.prompts().list().getOrThrow().size, equalTo(1))
        assertThat(
            mcpClient.prompts().get("prompt", PromptRequest(mapOf("a1" to "foo"))).getOrThrow().description,
            equalTo("description")
        )

        assertThat(
            mcpClient.resources().list().getOrThrow().size,
            equalTo(1)
        )

        assertThat(
            mcpClient.resources().read(ResourceRequest(Uri.of("https://http4k.org"))).getOrThrow(),
            equalTo(ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of("")))))
        )

        assertThat(
            mcpClient.completions()
                .complete(
                    CompletionRequest(
                        Reference.Resource(Uri.of("https://http4k.org")),
                        CompletionArgument("foo", "bar")
                    )
                ).getOrThrow(),
            equalTo(CompletionResponse(Completion(listOf("1", "2"))))
        )

        assertThat(mcpClient.tools().list().getOrThrow().size, equalTo(1))

        assertThat(
            mcpClient.tools().call(ToolName.of("reverse"), ToolRequest()).getOrThrow(),
            equalTo(ToolResponse.Ok(listOf(Content.Text("tseuqeRlooT"))))
        )

        mcpClient.sampling().sample(
            ModelIdentifier.of("asd"),
            SamplingRequest(listOfNotNull(), MaxTokens.of(123))
        ).map { it.getOrThrow() }.toList()

        mcpClient.stop()

        server.stop()
    }

    fun clientFor(uri: Uri): SseMcpClient
}
