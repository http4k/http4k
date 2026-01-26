package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Elicitation
import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.ElicitationModel
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.ToolName
import org.http4k.format.auto
import org.http4k.lens.with
import org.http4k.routing.bind
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * Contract for MCP clients that support streaming capabilities (sampling, elicitation, progress).
 */
interface McpStreamingClientContract<T> : McpClientContract<T> {

    @Test
    fun `can do sampling`() {
        val model = ModelName.of("my model")

        val samplingResponses = listOf(
            SamplingResponse(model, Assistant, Content.Text("hello"), null),
            SamplingResponse(model, Assistant, Content.Text("world"), StopReason.of("foobar"))
        )

        val tools = ServerTools(
            Tool("sample", "description") bind {
                val received = it.client.sample(
                    SamplingRequest(listOf(), MaxTokens.of(1)),
                    Duration.ofSeconds(1)
                ).toList()
                assertThat(received, equalTo(samplingResponses.map { Success(it) }))
                Ok(listOf(Content.Text(received.size.toString())))
            }
        )

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            tools = tools,
        )

        val server = toPolyHandler(protocol).asServer(JettyLoom(0)).start()
        val mcpClient = clientFor(server.port())

        mcpClient.start(Duration.ofSeconds(1))

        mcpClient.sampling().onSampled { samplingResponses.asSequence() }

        assertThat(
            mcpClient.tools().call(ToolName.of("sample"), ToolRequest(meta = Meta("sample"))),
            equalTo(Success(Ok(Content.Text("2"))))
        )

        assertThat(
            mcpClient.tools().call(ToolName.of("sample"), ToolRequest(meta = Meta("sample"))),
            equalTo(Success(Ok(Content.Text("2"))))
        )

        mcpClient.stop()
        server.stop()
    }

    class StreamingFooBar : ElicitationModel() {
        var foo by string("foo", "bar")
        var bar by optionalString("", "")
    }

    @Test
    fun `can do elicitation`() {
        val output = Elicitation.auto(StreamingFooBar()).toLens("name", "it's a name")

        val response = StreamingFooBar().apply { foo = "foo" }

        val tools = ServerTools(
            Tool("elicit", "description") bind {
                val request = ElicitationRequest.Form("foobar", output, progressToken = it.meta.progressToken)
                val received = it.client.elicit(request, Duration.ofSeconds(1))

                assertThat(
                    received,
                    equalTo(
                        Success(
                            ElicitationResponse(ElicitationAction.valueOf(it.meta.progressToken!!.toString())).with(
                                output of response
                            )
                        )
                    )
                )

                assertThat(output(received.valueOrNull()!!), equalTo(response))

                Ok(listOf(Content.Text(received.valueOrNull()!!.action.name)))
            }
        )

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            tools = tools,
        )

        val server = toPolyHandler(protocol).asServer(JettyLoom(0)).start()
        val mcpClient = clientFor(server.port())

        mcpClient.start(Duration.ofSeconds(1))

        mcpClient.elicitations().onElicitation {
            ElicitationResponse(ElicitationAction.valueOf(it.progressToken!!.toString())).with(output of response)
        }

        assertThat(
            mcpClient.tools().call(ToolName.of("elicit"), ToolRequest(meta = Meta("accept"))),
            equalTo(Success(Ok(Content.Text("accept"))))
        )

        assertThat(
            mcpClient.tools().call(ToolName.of("elicit"), ToolRequest(meta = Meta("decline"))),
            equalTo(Success(Ok(Content.Text("decline"))))
        )

        assertThat(
            mcpClient.tools().call(ToolName.of("elicit"), ToolRequest(meta = Meta("cancel"))),
            equalTo(Success(Ok(Content.Text("cancel"))))
        )

        mcpClient.stop()
        server.stop()
    }

    @Test
    fun `can do progress`() {
        val tools = ServerTools(
            Tool("progress", "description") bind {
                it.client.progress(1, 2.0)
                Ok(listOf(Content.Text("")))
            }
        )

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            tools = tools
        )

        val server = toPolyHandler(protocol).asServer(JettyLoom(0)).start()
        val mcpClient = clientFor(server.port())

        mcpClient.start(Duration.ofSeconds(1))

        val prog = AtomicReference<Progress>()
        mcpClient.progress().onProgress(fn = prog::set)

        assertThat(
            mcpClient.tools().call(ToolName.of("progress"), ToolRequest(meta = Meta("progress"))),
            equalTo(Success(Ok(Content.Text(""))))
        )

        assertThat(prog.get(), equalTo(Progress("progress", 1, 2.0)))

        mcpClient.stop()
        server.stop()
    }
}
