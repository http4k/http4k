package org.http4k.mcp.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MaxTokens
import org.http4k.connect.model.MimeType
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role.Companion.Assistant
import org.http4k.connect.model.StopReason
import org.http4k.connect.model.ToolName
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.int
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.client.McpError
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ProtocolCapability.Experimental
import org.http4k.mcp.protocol.ProtocolCapability.PromptsChanged
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.ServerCapabilities.PromptCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.server.capability.ServerCompletions
import org.http4k.mcp.server.capability.ServerPrompts
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerSampling
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.http.HttpStreamingMcp
import org.http4k.mcp.server.http.HttpStreamingSessions
import org.http4k.mcp.server.protocol.ClientRequestTarget.Entity
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.sessions.SessionProvider
import org.http4k.mcp.server.sse.SseMcp
import org.http4k.mcp.server.sse.SseSessions
import org.http4k.routing.bind
import org.http4k.routing.mcpSse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class TestMcpClientTest {

    private val serverName = McpEntity.of("server")
    private val random = Random(0)

    @Test
    fun `can use mcp client to connect and get responses`() {
        val capabilities = mcpSse(
            ServerMetaData(
                serverName, Version.of("1"),
                PromptsChanged,
                Experimental,
            ),
        )
            .testMcpSseClient().start()

        assertThat(
            capabilities, equalTo(
                Success(
                    ServerCapabilities(
                        prompts = PromptCapabilities(true),
                        experimental = Unit
                    )
                )
            )
        )
    }

    private val metadata = ServerMetaData(serverName, Version.of("1"))

    @Test
    fun `deal with prompts`() {
        val intArg = Prompt.Arg.int().required("name", "description")
        val prompt = Prompt(PromptName.of("prompt"), "description", intArg)

        val serverPrompts = ServerPrompts(
            listOf(
                prompt bind {
                    PromptResponse(
                        listOf(
                            Message(Assistant, Content.Text(intArg(it).toString().reversed()))
                        ),
                        "description",
                    )
                }
            )
        )
        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                prompts = serverPrompts, random = random
            )
        )

        mcp.useClient {
            assertThat(
                prompts().list(),
                equalTo(
                    Success(
                        listOf(
                            McpPrompt(
                                PromptName.of("prompt"), "description",
                                listOf(McpPrompt.Argument("name", "description", true))
                            )
                        )
                    )
                )
            )

            assertThat(
                prompts().get(prompt.name, PromptRequest(mapOf("name" to "123"))),
                equalTo(
                    Success(
                        PromptResponse(
                            listOf(Message(Assistant, Content.Text("321"))),
                            "description"
                        )
                    )
                )
            )

            assertThat(
                prompts().get(prompt.name, PromptRequest(mapOf("name" to "asd"))),
                equalTo(Failure(McpError.Protocol(InvalidParams)))
            )

            serverPrompts.items = emptyList()

            prompts().expectNotification()
        }
    }


    @Test
    fun `deal with static resources`() {
        val resource = Resource.Static(Uri.of("https://www.http4k.org"), ResourceName.of("HTTP4K"), "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uri)

        val serverResources = ServerResources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                resources = serverResources,
                random = random
            )
        )

        mcp.useClient {
            assertThat(
                resources().list(), equalTo(
                    Success(
                        listOf(
                            McpResource(
                                resource.uri,
                                null,
                                ResourceName.of("HTTP4K"),
                                "description",
                                null
                            )
                        )
                    )
                )
            )

            assertThat(
                resources().read(ResourceRequest(resource.uri)),
                equalTo(Success(ResourceResponse(listOf(content))))
            )

            var calls = 0

            resources().subscribe(resource.uri) { calls++ }

            serverResources.triggerUpdated(resource.uri)

            resources().expectSubscriptionNotification(resource.uri)

            assertThat(calls, equalTo(1))

            resources().unsubscribe(resource.uri)
            serverResources.triggerUpdated(resource.uri)
            assertThat(calls, equalTo(1))

            serverResources.items = emptyList()

            resources().expectNotification()
        }
    }

    @Test
    fun `deal with templated resources`() {
        val resource =
            Resource.Templated(Uri.of("https://www.http4k.org/{+template}"), ResourceName.of("HTTP4K"), "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uriTemplate)

        val serverResources = ServerResources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                resources = serverResources,
                random = random
            )
        )

        mcp.useClient {
            assertThat(resources().list(), equalTo(Success(emptyList())))

            assertThat(
                resources().read(ResourceRequest(resource.uriTemplate)),
                equalTo(Success(ResourceResponse(listOf(content))))
            )
        }
    }

    @Test
    fun `deal with tools`() {
        val stringArg = Tool.Arg.required("foo", "description1")
        val intArg = Tool.Arg.int().optional("bar", "description2")

        val tool = Tool("name", "description", stringArg, intArg)

        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val serverTools = ServerTools(listOf(tool bind { it ->
            ToolResponse.Ok(listOf(content, Content.Text(stringArg(it) + intArg(it))))
        }))

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                tools = serverTools,
                random = random
            )
        )

        mcp.useClient {
            assertThat(
                tools().list(),
                equalTo(
                    Success(
                        listOf(
                            McpTool(
                                ToolName.of("name"), "description",
                                mapOf(
                                    "type" to "object",
                                    "required" to listOf("foo"),
                                    "properties" to mapOf(
                                        "foo" to mapOf("type" to "string", "description" to "description1"),
                                        "bar" to mapOf("type" to "integer", "description" to "description2")
                                    )
                                )
                            )
                        )
                    )
                )
            )

            assertThat(
                tools().call(tool.name, ToolRequest(mapOf("foo" to "foo", "bar" to 123))),
                equalTo(Success(ToolResponse.Ok(listOf(content, Content.Text("foo123")))))
            )

            assertThat(
                tools().call(tool.name, ToolRequest(mapOf("foo" to "foo", "bar" to "notAnInt"))),
                equalTo(Failure(McpError.Protocol(InvalidParams)))
            )

            val latch = CountDownLatch(1)

            tools().onChange(latch::countDown)

            serverTools.items = emptyList()

            tools().expectNotification()

            latch.await()
        }
    }

    @Test
    fun `deal with completions`() {
        val ref = Reference.Resource(Uri.of("https://www.http4k.org"))
        val serverCompletions = ServerCompletions(
            listOf(ref bind { CompletionResponse(listOf("values"), 1, true) })
        )

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                completions = serverCompletions,
                random = random
            )
        )

        mcp.useClient {
            assertThat(
                completions().complete(CompletionRequest(ref, CompletionArgument("arg", "value"))),
                equalTo(Success(CompletionResponse(listOf("values"), 1, true)))
            )
        }
    }

    @Test
    @Disabled // TODO replace
    fun `deal with client sampling in http streaming`() {
        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val model = ModelName.of("name")
        val serverSampling = ServerSampling()

        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, HttpStreamingSessions(SessionProvider.Random(random)),
                sampling = serverSampling,
                tools = ServerTools(
                    Tool("sample", "description") bind { it ->
                        val received = serverSampling.sampleClient(
                            Entity(metadata.entity.name),
                            SamplingRequest(listOf(), MaxTokens.of(1), progressToken = it.progressToken!!),
                            Duration.ofSeconds(5)
                        ).toList()

                        assertThat(
                            received, equalTo(
                                listOf(
                                    Success(SamplingResponse(model, Assistant, content, null)),
                                    Success(SamplingResponse(model, Assistant, content, StopReason.of("bored")))
                                )
                            )
                        )

                        ToolResponse.Ok(listOf(Content.Text(received.size.toString())))
                    }
                ),
                random = random
            ),
        )

        mcp.useClient {
            sampling().onSampled {
                sequenceOf(
                    SamplingResponse(model, Assistant, content, null),
                    SamplingResponse(model, Assistant, content, StopReason.of("bored")),
                    SamplingResponse(model, Assistant, content, StopReason.of("this should not be processed"))
                )
            }

            sampling().start()

            val received = serverSampling
                .sampleClient(
                    Entity(metadata.entity.name),
                    SamplingRequest(listOf(), MaxTokens.of(1)), Duration.ofSeconds(5)
                )

            assertThat(
                received.toList(), equalTo(
                    listOf(
                        Success(SamplingResponse(model, Assistant, content, null)),
                        Success(SamplingResponse(model, Assistant, content, StopReason.of("bored")))
                    )
                )
            )
        }
    }

    private fun PolyHandler.useClient(fn: TestMcpClient.() -> Unit) {
        testMcpSseClient().use {
            it.start()
            it.fn()
        }
    }
}
