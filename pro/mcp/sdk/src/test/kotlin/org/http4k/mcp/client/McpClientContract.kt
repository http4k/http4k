package org.http4k.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.lens.with
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Role.assistant
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Tool
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.capability.ServerCompletions
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.protocol.Completions
import org.http4k.mcp.server.protocol.Tools
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Resources
import org.http4k.routing.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

interface McpClientContract<R : Any, P : McpProtocol<*, R>> : PortBasedTest {

    val notifications: Boolean

    @Test
    fun `can interact with server`() {
        val model = ModelIdentifier.of("my model")
        // TODO client sampling if supported
        val samplingResponses = listOf(
            SamplingResponse(model, assistant, Content.Text("hello"), null),
            SamplingResponse(model, assistant, Content.Text("world"), StopReason.of("foobar"))
        )

        val toolArg = Tool.Arg.required("name")
        val tools = ServerTools(Tool("reverse", "description", toolArg) bind {
            ToolResponse.Ok(listOf(Content.Text(toolArg(it).reversed())))
        })

        val protocol = protocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            Prompts(Prompt(PromptName.of("prompt"), "description1") bind {
                PromptResponse(listOf(Message(assistant, Content.Text(it.toString()))), "description")
            }),
            tools,
            ServerResources(Resource.Static(Uri.of("https://http4k.org"), ResourceName.of("HTTP4K"), "description") bind {
                ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of(""))))
            }),
            ServerCompletions(Reference.Resource(Uri.of("https://http4k.org")) bind {
                CompletionResponse(Completion(listOf("1", "2")))
            })
        )

        val server = toPolyHandler(protocol).asServer(Helidon(0)).start()

//        protocol.start()

        val mcpClient = clientFor(server.port())

        val latch = CountDownLatch(1)

        if (notifications) {
            mcpClient.tools().onChange {
                latch.countDown()
            }
        }

        mcpClient.start()

        assertThat(mcpClient.prompts().list().valueOrNull()!!.size, equalTo(1))

        assertThat(
            mcpClient.prompts().get(PromptName.of("prompt"), PromptRequest(mapOf("a1" to "foo")))
                .valueOrNull()!!.description,
            equalTo("description")
        )

        assertThat(
            mcpClient.resources().list().valueOrNull()!!.size,
            equalTo(1)
        )

        assertThat(
            mcpClient.resources().read(ResourceRequest(Uri.of("https://http4k.org"))).valueOrNull()!!,
            equalTo(ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of("")))))
        )

        assertThat(
            mcpClient.completions()
                .complete(
                    CompletionRequest(
                        Reference.Resource(Uri.of("https://http4k.org")),
                        CompletionArgument("foo", "bar")
                    )
                ).valueOrNull()!!,
            equalTo(CompletionResponse(Completion(listOf("1", "2"))))
        )

        assertThat(mcpClient.tools().list().valueOrNull()!!.size, equalTo(1))

        assertThat(
            mcpClient.tools().call(ToolName.of("reverse"), ToolRequest().with(toolArg of "foobar")).valueOrNull()!!,
            equalTo(ToolResponse.Ok(listOf(Content.Text("raboof"))))
        )

        // TODO test client sampling
//        assertThat(
//            mcpClient.sampling().sample(
//                ModelIdentifier.of("asd"),
//                SamplingRequest(listOfNotNull(), MaxTokens.of(123))
//            ).map { it.valueOrNull()!! }.toList(), equalTo(samplingResponses)
//        )

        if (notifications) {
            tools.items = emptyList()

            latch.await()

            assertThat(mcpClient.tools().list().valueOrNull()!!.size, equalTo(0))
        }

        mcpClient.stop()
        server.stop()
    }

    fun protocol(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions
    ): P

    fun toPolyHandler(protocol: P): PolyHandler

    fun clientFor(port: Int): McpClient
}
