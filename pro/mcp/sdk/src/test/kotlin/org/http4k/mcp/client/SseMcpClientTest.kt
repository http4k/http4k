package org.http4k.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.failureOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.lens.with
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Role.assistant
import org.http4k.mcp.model.Tool
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.sse.SseClientSessions
import org.http4k.mcp.server.sse.StandardSseMcp
import org.http4k.routing.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.Sse
import org.junit.jupiter.api.Test

class SseMcpClientTest : McpClientContract<Sse, Response> {

    override val notifications = true

    override fun clientFor(port: Int) = SseMcpClient(
        clientName, Version.of("1.0.0"),
        ClientCapabilities(),
        Request(GET, Uri.of("http://localhost:${port}/sse")),
        JavaHttpClient(responseBodyMode = Stream)
    )

    override fun clientSessions() = SseClientSessions().apply { start() }

    override fun toPolyHandler(protocol: McpProtocol<Sse, Response>) = StandardSseMcp(protocol)

    @Test
    fun `deals with error`() {
        val toolArg = Tool.Arg.required("name")

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            Prompt(PromptName.of("prompt"), "description1") bind {
                PromptResponse(listOf(Message(assistant, Content.Text(it.toString()))), "description")
            },
            Tool("reverse", "description", toolArg) bind {
                ToolResponse.Ok(listOf(Content.Text(toolArg(it).reversed())))
            },
            Resource.Static(Uri.of("https://http4k.org"), ResourceName.of("HTTP4K"), "description") bind {
                ResourceResponse(listOf(Resource.Content.Text("foo", Uri.of(""))))
            },
            Reference.Resource(Uri.of("https://http4k.org")) bind {
                CompletionResponse(Completion(listOf("1", "2")))
            }
        )

        val server = blowUpWhenBoom().then(toPolyHandler(protocol))
            .asServer(Helidon(0)).start()

        val mcpClient = clientFor(server.port())

        mcpClient.start()

        assertThat(
            mcpClient.tools().call(ToolName.of("reverse"), ToolRequest().with(toolArg of "boom")).failureOrNull(),
            present()
        )

        mcpClient.stop()
        server.stop()
    }

    private fun blowUpWhenBoom() = Filter { next ->
        {
            if (it.bodyString().contains("boom")) Response(INTERNAL_SERVER_ERROR)
            else next(it)
        }
    }

}
