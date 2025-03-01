package org.http4k.mcp.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.int
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpError
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Role
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
import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.session.McpSession
import org.http4k.mcp.server.sse.Sse
import org.http4k.mcp.server.sse.StandardMcpSse
import org.http4k.routing.bind
import org.http4k.routing.mcpSse
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class TestMcpClientTest {

    @Test
    fun `can use mcp client to connect and get responses`() {
        val capabilities = mcpSse(
            ServerMetaData(
                McpEntity.of("my mcp server"), Version.of("1"),
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

    private val metadata = ServerMetaData(McpEntity.of("server"), Version.of("1"))

    @Test
    fun `deal with prompts`() {
        val intArg = Prompt.Arg.int().required("name", "description")
        val prompt = Prompt(PromptName.of("prompt"), "description", intArg)

        val mcp = StandardMcpSse(
            RealtimeMcpProtocol(
                McpSession.Sse(),
                metadata, prompts = Prompts(
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
                ), random = Random(0)))

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
        }
    }


    @Test
    fun `deal with static resources`() {
        val resource = Resource.Static(Uri.of("https://www.http4k.org"), ResourceName.of("HTTP4K"), "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uri)

        val resources = Resources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp =
            StandardMcpSse(RealtimeMcpProtocol(McpSession.Sse(), metadata, resources = resources, random = Random(0)))

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


            /**
             *
             *             mcp.sendToMcp(McpResource.Subscribe, McpResource.Subscribe.Request(resource.uri))
             *
             *             resources.triggerUpdated(resource.uri)
             *
             *             assertNextMessage(McpResource.Updated, McpResource.Updated.Notification(resource.uri))
             *
             *             mcp.sendToMcp(McpResource.Unsubscribe, McpResource.Unsubscribe.Request(resource.uri))
             *
             *             resources.triggerUpdated(resource.uri)
             *
             *             assertNoResponse()
             *
             */
        }
    }


    @Test
    fun `deal with templated resources`() {
        val resource =
            Resource.Templated(Uri.of("https://www.http4k.org/{+template}"), ResourceName.of("HTTP4K"), "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uriTemplate)

        val resources = Resources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp =
            StandardMcpSse(RealtimeMcpProtocol(McpSession.Sse(), metadata, resources = resources, random = Random(0)))

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

        val tools = Tools(listOf(tool bind {
            ToolResponse.Ok(listOf(content, Content.Text(stringArg(it) + intArg(it))))
        }))

        val mcp = StandardMcpSse(RealtimeMcpProtocol(McpSession.Sse(), metadata, tools = tools, random = Random(0)))

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

            tools.items = emptyList()

            latch.await()
        }

//
//        assertNextMessage(McpTool.List.Changed, McpTool.List.Changed.Notification)
    }

    @Test
    fun `deal with completions`() {
        val ref = Reference.Resource(Uri.of("https://www.http4k.org"))
        val completions = Completions(
            listOf(ref bind { CompletionResponse(Completion(listOf("values"), 1, true)) })
        )

        val mcp = StandardMcpSse(
            RealtimeMcpProtocol(
                McpSession.Sse(),
                metadata,
                completions = completions,
                random = Random(0)
            )
        )

        mcp.useClient {
            assertThat(
                completions().complete(CompletionRequest(ref, CompletionArgument("arg", "value"))),
                equalTo(Success(CompletionResponse(Completion(listOf("values"), 1, true))))
            )
        }
    }
//
//    @Test
//    fun `deal with incoming sampling`() {
//        val content1 = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))
//        val content2 = Content.Text("this is the end!")
//
//        val model = ModelIdentifier.of("name")
//        val sampling = IncomingSampling(
//            listOf(
//                ModelSelector(model) { MAX } bind {
//                    listOf(
//                        SamplingResponse(model, Role.assistant, content1, null),
//                        SamplingResponse(model, Role.assistant, content2, StopReason.of("bored"))
//                    ).asSequence()
//                }
//            ))
//
//        val mcp = StandardMcpSse(RealtimeMcpProtocol(McpSession.Sse(), metadata, incomingSampling = sampling, random = Random(0)))
//
//        with(mcp.testSseClient(Request(GET, "/sse"))) {
//            assertInitializeLoop(mcp)
//
//            mcp.sendToMcp(McpSampling, McpSampling.Request(listOf(), MaxTokens.of(1)))
//
//            assertNextMessage(McpSampling.Response(model, null, Role.assistant, content1))
//
//            assertNextMessage(McpSampling.Response(model, StopReason.of("bored"), Role.assistant, content2))
//        }
//    }
//
//    @Test
//    fun `deal with outgoing sampling`() {
//        val received = mutableListOf<SamplingResponse>()
//
//        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))
//
//        val model = ModelIdentifier.of("name")
//        val sampling = OutgoingSampling(
//            listOf(
//                clientName bind { received += it }
//            ))
//
//        val mcp = StandardMcpSse(RealtimeMcpProtocol(McpSession.Sse(), metadata, outgoingSampling = sampling, random = Random(0)))
//
//        with(mcp.testSseClient(Request(GET, "/sse"))) {
//            assertInitializeLoop(mcp)
//
//            sampling.sample(
//                serverName, SamplingRequest(
//                    listOf(), MaxTokens.of(1),
//                    connectRequest = Request(GET, "")
//                ), RequestId.of(1)
//            )
//
//            assertNextMessage(
//                McpSampling,
//                McpSampling.Request(listOf(), MaxTokens.of(1)),
//                RequestId.of(1)
//            )
//
//            mcp.sendToMcp(
//                McpSampling.Response(model, null, Role.assistant, content),
//                RequestId.of(1)
//            )
//
//            mcp.sendToMcp(
//                McpSampling.Response(model, StopReason.of("bored"), Role.assistant, content),
//                RequestId.of(1)
//            )
//
//            // this is ignored!
//            mcp.sendToMcp(
//                McpSampling.Response(model, StopReason.of("another stop reason"), Role.assistant, content),
//                RequestId.of(1)
//            )
//
//            assertThat(
//                received, equalTo(
//                    listOf(
//                        SamplingResponse(model, Role.assistant, content, null),
//                        SamplingResponse(model, Role.assistant, content, StopReason.of("bored"))
//                    )
//                )
//            )
//        }
//    }

    private fun PolyHandler.useClient(fn: McpClient.() -> Unit) {
        testMcpClient().use {
            it.start()
            it.fn()
        }
    }
}
