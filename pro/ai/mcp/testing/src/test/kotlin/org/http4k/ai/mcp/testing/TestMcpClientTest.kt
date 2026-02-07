package org.http4k.ai.mcp.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.ToolName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.int
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.ai.mcp.model.TaskSupport
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.ToolChoice
import org.http4k.ai.mcp.model.ToolChoiceMode
import org.http4k.ai.mcp.model.ToolExecution
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerCapabilities
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.Experimental
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.PromptsChanged
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.capability.ServerCompletions
import org.http4k.ai.mcp.server.capability.ServerPrompts
import org.http4k.ai.mcp.server.capability.ServerResources
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.mcp.server.http.HttpStreamingMcp
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.server.sessions.SessionProvider
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class TestMcpClientTest {

    private val serverName = McpEntity.of("server")
    private val random = Random(0)

    @Test
    fun `can use mcp client to connect and get responses`() {
        val capabilities = mcpHttpStreaming(
            ServerMetaData(
                serverName, Version.of("1"),
                PromptsChanged,
                Experimental,
            ),
            NoMcpSecurity,
        )
            .testMcpClient().start()

        assertThat(
            capabilities, equalTo(
                Success(
                    ServerCapabilities(PromptsChanged, Experimental)
                )
            )
        )
    }

    private val metadata = ServerMetaData(serverName, Version.of("1"))

    @Test
    fun `deal with prompts`() {
        val intArg = Prompt.Arg.int().required("name", "description", mapOf("title" to "title"))
        val icons = listOf(org.http4k.ai.mcp.model.Icon(Uri.of("https://example.com/icon.png")))
        val prompt = Prompt(PromptName.of("prompt"), "description", intArg, title = "title", icons = icons)

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
        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, HttpSessions(SessionProvider.Random(random)),
                prompts = serverPrompts, random = random
            ), NoMcpSecurity
        ).testMcpClient()

        mcp.useClient {
            assertThat(
                prompts().list(),
                equalTo(
                    Success(
                        listOf(
                            McpPrompt(
                                PromptName.of("prompt"), "description", "title",
                                listOf(McpPrompt.Argument("name", "description", "title", true)),
                                icons
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
        val icons = listOf(org.http4k.ai.mcp.model.Icon(Uri.of("https://example.com/icon.png")))
        val resource = Resource.Static(Uri.of("https://www.http4k.org"), ResourceName.of("HTTP4K"), "description", icons = icons)
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uri)

        val serverResources = ServerResources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, HttpSessions(SessionProvider.Random(random)),
                resources = serverResources,
                random = random
            ),
            NoMcpSecurity
        ).testMcpClient()

        mcp.useClient {
            assertThat(
                resources().list(), equalTo(
                    Success(listOf(McpResource(resource.uri, ResourceName.of("HTTP4K"), "description", icons = icons)))
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

        val uri = Uri.of("https://www.http4k.org/value")
        val resource =
            Resource.Templated(
                ResourceUriTemplate.of("https://www.http4k.org/{+template}"),
                ResourceName.of("HTTP4K"),
                "description"
            )
        val content = Resource.Content.Blob(Base64Blob.encode("image"), uri)

        val serverResources = ServerResources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, HttpSessions(SessionProvider.Random(random)),
                resources = serverResources,
                random = random
            ),
            NoMcpSecurity
        ).testMcpClient()

        mcp.useClient {
            assertThat(resources().list(), equalTo(Success(emptyList())))

            assertThat(
                resources().read(ResourceRequest(uri)),
                equalTo(Success(ResourceResponse(listOf(content))))
            )
        }
    }

    @Test
    fun `deal with tools`() {
        val stringArg = Tool.Arg.string().required("foo", "description1")
        val intArg = Tool.Arg.int().optional("bar", "description2")
        val icons = listOf(org.http4k.ai.mcp.model.Icon(Uri.of("https://example.com/icon.png")))

        val tool = Tool(
            "name",
            "description",
            stringArg,
            intArg,
            title = "title",
            icons = icons,
            execution = ToolExecution(TaskSupport.optional)
        )

        val content =
            Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val serverTools = ServerTools(listOf(tool bind {
            it.meta.progressToken?.let { p ->
                it.client.progress(1, 5.0)
                it.client.progress(2, 5.0)
            }

            ToolResponse.Ok(listOf(content, Content.Text(stringArg(it) + intArg(it))))
        }))

        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, HttpSessions(SessionProvider.Random(random)),
                tools = serverTools,
                random = random
            ),
            NoMcpSecurity
        ).testMcpClient()

        mcp.useClient {
            assertThat(
                tools().list(),
                equalTo(
                    Success(
                        listOf(
                            McpTool(
                                ToolName.of("name"), "description", "title",
                                mapOf(
                                    "type" to "object",
                                    "required" to listOf("foo"),
                                    "properties" to mapOf(
                                        "foo" to mapOf("type" to "string", "description" to "description1"),
                                        "bar" to mapOf("type" to "integer", "description" to "description2")
                                    )
                                ),
                                null,
                                null,
                                icons,
                                ToolExecution(TaskSupport.optional)
                            )
                        )
                    )
                )
            )

            var progress = 0
            progress().onProgress {
                progress++
            }

            assertThat(
                tools().call(tool.name, ToolRequest(mapOf("foo" to "foo", "bar" to 123), meta = Meta("foobar"))),
                equalTo(Success(ToolResponse.Ok(listOf(content, Content.Text("foo123")))))
            )

            assertThat(progress, equalTo(2))

            assertThat(
                tools().call(tool.name, ToolRequest(mapOf("foo" to "foo", "bar" to "notAnInt"))),
                equalTo(Failure(McpError.Protocol(InvalidParams)))
            )

            val toolsLatch = CountDownLatch(1)

            tools().onChange(toolsLatch::countDown)

            serverTools.items = emptyList()

            tools().expectNotification()

            toolsLatch.await()
        }
    }

    @Test
    fun `deal with completions`() {
        val ref = Reference.ResourceTemplate(Uri.of("https://www.http4k.org"))
        val serverCompletions = ServerCompletions(
            listOf(ref bind { CompletionResponse(listOf("values"), 1, true) })
        )

        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, HttpSessions(SessionProvider.Random(random)),
                completions = serverCompletions,
                random = random
            ),
            NoMcpSecurity
        ).testMcpClient()

        mcp.useClient {
            assertThat(
                completions().complete(ref, CompletionRequest(CompletionArgument("arg", "value"))),
                equalTo(Success(CompletionResponse(listOf("values"), 1, true)))
            )
        }
    }

    @Test
    fun `deal with progress`() {
        val ref = Reference.ResourceTemplate(Uri.of("https://www.http4k.org"))

        val progress = Progress("hello", 1, 1.0)
        val serverCompletions = ServerCompletions(
            listOf(ref bind {
                it.client.progress(progress.progress, progress.total)

                CompletionResponse(listOf("values"), 1, true)
            })
        )

        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, HttpSessions(SessionProvider.Random(random)),
                completions = serverCompletions,
                random = random
            ),
            NoMcpSecurity
        ).testMcpClient()

        mcp.useClient {
            val latch = CountDownLatch(1)
            progress().onProgress {
                assertThat(it, equalTo(progress))
                latch.countDown()
            }

            assertThat(
                completions().complete(
                    ref,
                    CompletionRequest(CompletionArgument("arg", "value"), meta = Meta("hello"))
                ),
                equalTo(Success(CompletionResponse(listOf("values"), 1, true)))
            )

            latch.await()
        }
    }

    @Test
    fun `deal with client sampling`() {
        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val model = ModelName.of("name")

        val testTool = McpTool(
            ToolName.of("test_tool"),
            "test tool description",
            null,
            emptyMap(),
            null,
            null
        )

        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, HttpSessions(SessionProvider.Random(random)),
                tools = ServerTools(
                    Tool("sample", "description") bind {
                        val samplingRequest = it.client.sample(
                            SamplingRequest(
                                messages = listOf(),
                                maxTokens = MaxTokens.of(1),
                                tools = listOf(testTool),
                                toolChoice = ToolChoice(ToolChoiceMode.auto)
                            ),
                            Duration.ofSeconds(1)
                        ).toList()
                        ToolResponse.Ok(listOf(Content.Text(samplingRequest.size.toString())))
                    }
                ),
                random = random
            ),
            NoMcpSecurity
        ).testMcpClient()

        mcp.useClient {
            sampling().onSampled { request ->
                assertThat(request.tools, equalTo(listOf(testTool)))
                assertThat(request.toolChoice, equalTo(ToolChoice(ToolChoiceMode.auto)))

                sequenceOf(
                    SamplingResponse.Ok(model, Assistant, listOf(content), null),
                    SamplingResponse.Ok(model, Assistant, listOf(content), StopReason.of("bored")),
                    SamplingResponse.Ok(
                        model,
                        Assistant,
                        listOf(content),
                        StopReason.of("this should not be processed")
                    )
                )
            }
            assertThat(
                tools().call(ToolName.of("sample"), ToolRequest(meta = Meta("hello"))),
                equalTo(Success(ToolResponse.Ok(listOf(Content.Text("1")))))
            )
        }
    }
}
