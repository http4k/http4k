package org.http4k.mcp.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.model.Base64Blob
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
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Tool
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ProtocolCapability.Experimental
import org.http4k.mcp.protocol.ProtocolCapability.PromptsChanged
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.ServerCapabilities.PromptCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Sampling
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.SessionProvider
import org.http4k.mcp.server.sse.SseTransport
import org.http4k.mcp.server.sse.StandardSseMcp
import org.http4k.routing.bind
import org.http4k.routing.mcpSse
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class TestMcpClientTest {

    private val serverName = McpEntity.of("server")
    private val clientName = McpEntity.of("client")
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
            .testMcpClient().start()

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

        val serverPrompts = Prompts(
            listOf(
                prompt bind {
                    PromptResponse(
                        listOf(
                            Message(Role.assistant, Content.Text(intArg(it).toString().reversed()))
                        ),
                        "description",
                    )
                }
            )
        )
        val mcp = StandardSseMcp(
            McpProtocol(
                SseTransport(SessionProvider.Random(random)), metadata,
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
                            listOf(Message(Role.assistant, Content.Text("321"))),
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

        val serverResources = Resources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = StandardSseMcp(
            McpProtocol(
                SseTransport(SessionProvider.Random(random)), metadata,
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

        val serverResources = Resources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = StandardSseMcp(
            McpProtocol(
                SseTransport(SessionProvider.Random(random)), metadata,
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

        val serverTools = Tools(listOf(tool bind {
            ToolResponse.Ok(listOf(content, Content.Text(stringArg(it) + intArg(it))))
        }))

        val mcp = StandardSseMcp(
            McpProtocol(
                SseTransport(SessionProvider.Random(random)), metadata,
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
        val serverCompletions = Completions(
            listOf(ref bind { CompletionResponse(Completion(listOf("values"), 1, true)) })
        )

        val mcp = StandardSseMcp(
            McpProtocol(
                SseTransport(SessionProvider.Random(random)), metadata,
                completions = serverCompletions,
                random = random
            )
        )

        mcp.useClient {
            assertThat(
                completions().complete(CompletionRequest(ref, CompletionArgument("arg", "value"))),
                equalTo(Success(CompletionResponse(Completion(listOf("values"), 1, true))))
            )
        }
    }

    @Test
    fun `deal with client sampling`() {
        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val model = ModelIdentifier.of("name")
        val serverSampling = Sampling()

        val mcp = StandardSseMcp(
            McpProtocol(
                SseTransport(SessionProvider.Random(random)), metadata,
                sampling = serverSampling,
                random = random
            )
        )

        mcp.useClient {
            sampling().onSampled {
                sequenceOf(
                    SamplingResponse(model, Role.assistant, content, null),
                    SamplingResponse(model, Role.assistant, content, StopReason.of("bored")),
                    SamplingResponse(model, Role.assistant, content, StopReason.of("this should not be processed"))
                )
            }

            val received = serverSampling
                .sampleClient(clientName, SamplingRequest(listOf(), MaxTokens.of(1)), RequestId.of(1))

            sampling().expectSamplingRequest()

            assertThat(
                received.toList(), equalTo(
                    listOf(
                        Success(SamplingResponse(model, Role.assistant, content, null)),
                        Success(SamplingResponse(model, Role.assistant, content, StopReason.of("bored")))
                    )
                )
            )
        }
    }

    private fun PolyHandler.useClient(fn: TestMcpClient.() -> Unit) {
        testMcpClient().use {
            it.start()
            it.fn()
        }
    }
}
