package org.http4k.mcp.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiString
import org.http4k.format.renderError
import org.http4k.format.renderNotification
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.hamkrest.hasStatus
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.int
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SampleRequest
import org.http4k.mcp.SampleResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.capability.Completions
import org.http4k.mcp.capability.IncomingSampling
import org.http4k.mcp.capability.Logger
import org.http4k.mcp.capability.OutgoingSampling
import org.http4k.mcp.capability.Prompts
import org.http4k.mcp.capability.Resources
import org.http4k.mcp.capability.Roots
import org.http4k.mcp.capability.Tools
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.ModelScore.Companion.MAX
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.Root
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Tool
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolCapability
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-10-07`
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.HasMethod
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpLogging
import org.http4k.mcp.protocol.messages.McpNotification
import org.http4k.mcp.protocol.messages.McpPing
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpRequest
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpResponse
import org.http4k.mcp.protocol.messages.McpRoot
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.protocol.messages.ServerMessage
import org.http4k.mcp.sse.SseMcpProtocol
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.routing.bind
import org.http4k.sse.SseMessage
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random


class McpHandlerTest {
    private val serverName = McpEntity.of("server")
    private val clientName = McpEntity.of("server")

    private val metadata = ServerMetaData(serverName, Version.of("1"))

    @Test
    fun `performs init loop on startup`() {
        val mcp = McpHandler(SseMcpProtocol(metadata, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpPing, McpPing.Request)

            assertNextMessage(ServerMessage.Response.Empty)
        }
    }

    @Test
    fun `update roots`() {
        val roots = Roots()

        val mcp = McpHandler(SseMcpProtocol(metadata, roots = roots, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpRoot.Changed())

            assertNextMessage(McpRoot.List, McpRoot.List.Request(), RequestId.of(8299741232644920))

            val newRoots = listOf(Root(Uri.of("asd"), "name"))

            mcp.sendToMcp(McpRoot.List.Response(newRoots), RequestId.of(8299741232644920))

            assertThat(roots.toList(), equalTo(newRoots))
        }
    }

    @Test
    fun `deal with prompts`() {
        val intArg = Prompt.Arg.int().required("name", "description")
        val prompt = Prompt("prompt", "description", intArg)

        val mcp = McpHandler(SseMcpProtocol(metadata, prompts = Prompts(
            listOf(
                prompt bind {
                    PromptResponse(
                        "description",
                        listOf(
                            Message(Role.assistant, Content.Text(intArg(it).toString().reversed()))
                        )
                    )
                }
            )
        ), random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpPrompt.List, McpPrompt.List.Request())

            assertNextMessage(
                McpPrompt.List.Response(
                    listOf(
                        McpPrompt(
                            "prompt", "description",
                            listOf(McpPrompt.Argument("name", "description", true))
                        )
                    )
                )
            )

            mcp.sendToMcp(McpPrompt.Get, McpPrompt.Get.Request(prompt.name, mapOf("name" to "123")))

            assertNextMessage(
                McpPrompt.Get.Response(
                    listOf(Message(Role.assistant, Content.Text("321"))),
                    "description"
                )
            )

            mcp.sendToMcp(McpPrompt.Get, McpPrompt.Get.Request(prompt.name, mapOf("name" to "notAnInt")))

            assertNextMessage(InvalidParams)
        }
    }

    @Test
    fun `deal with static resources`() {
        val resource = Resource.Static(Uri.of("https://www.http4k.org"), "HTTP4K", "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uri)

        val resources = Resources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = McpHandler(SseMcpProtocol(metadata, resources = resources, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpResource.List, McpResource.List.Request())

            assertNextMessage(
                McpResource.List.Response(
                    listOf(
                        McpResource(
                            resource.uri,
                            null,
                            "HTTP4K",
                            "description",
                            null
                        )
                    )
                )
            )

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(resource.uri))

            assertNextMessage(McpResource.Read.Response(listOf(content)))

            mcp.sendToMcp(McpResource.Subscribe, McpResource.Subscribe.Request(resource.uri))

            resources.triggerUpdated(resource.uri)

            assertNextMessage(McpResource.Updated(resource.uri))

            mcp.sendToMcp(McpResource.Unsubscribe, McpResource.Unsubscribe.Request(resource.uri))

            resources.triggerUpdated(resource.uri)

            assertNoResponse()
        }
    }

    private fun TestSseClient.assertNoResponse() =
        assertThrows<NoSuchElementException> { received().first() }

    @Test
    fun `deal with templated resources`() {
        val resource = Resource.Templated(Uri.of("https://www.http4k.org/{+template}"), "HTTP4K", "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uriTemplate)

        val resources = Resources(listOf(resource bind { ResourceResponse(listOf(content)) }))
        val mcp = McpHandler(SseMcpProtocol(metadata, resources = resources, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpResource.List, McpResource.List.Request())

            assertNextMessage(McpResource.List.Response(listOf()))

            mcp.sendToMcp(McpResource.Template.List, McpResource.Template.List.Request(null))

            assertNextMessage(
                McpResource.Template.List.Response(
                    listOf(
                        McpResource(
                            null,
                            resource.uriTemplate,
                            "HTTP4K",
                            "description",
                            null
                        )
                    )
                )
            )

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(resource.uriTemplate))

            assertNextMessage(McpResource.Read.Response(listOf(content)))
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

        val mcp = McpHandler(SseMcpProtocol(metadata, tools = tools, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpTool.List, McpTool.List.Request())

            assertNextMessage(
                McpTool.List.Response(
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

            mcp.sendToMcp(
                McpTool.Call,
                McpTool.Call.Request(tool.name, mapOf("foo" to MoshiString("foo"), "bar" to MoshiInteger(123)))
            )

            assertNextMessage(McpTool.Call.Response(listOf(content, Content.Text("foo123"))))

            mcp.sendToMcp(
                McpTool.Call,
                McpTool.Call.Request(tool.name, mapOf("foo" to MoshiString("foo"), "bar" to MoshiString("notAnInt")))
            )

            assertNextMessage(McpTool.Call.Response(listOf(Content.Text("ERROR: -32602 Invalid params")), true))

            tools.items = emptyList()

            assertNextMessage(McpTool.List.Changed())
        }
    }

    @Test
    fun `deal with logger`() {
        val logger = Logger()
        val mcp = McpHandler(SseMcpProtocol(metadata, logger = logger, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)
            logger.log(sessionId, LogLevel.info, "message", emptyMap())

            assertNoResponse()

            mcp.sendToMcp(McpLogging.SetLevel, McpLogging.SetLevel.Request(LogLevel.debug))

            logger.log(sessionId, LogLevel.info, "message", emptyMap())

            assertNextMessage(McpLogging.LoggingMessage(LogLevel.info, "message", emptyMap()))
        }
    }


    @Test
    fun `deal with completions`() {
        val ref = Reference.Resource(Uri.of("https://www.http4k.org"))
        val completions = Completions(
            listOf(ref bind { CompletionResponse(Completion(listOf("values"), 1, true)) })
        )

        val mcp = McpHandler(SseMcpProtocol(metadata, completions = completions, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpCompletion, McpCompletion.Request(ref, CompletionArgument("arg", "value")))

            assertNextMessage(McpCompletion.Response(Completion(listOf("values"), 1, true)))
        }
    }

    @Test
    fun `deal with incoming sampling`() {
        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val model = ModelIdentifier.of("name")
        val sampling = IncomingSampling(listOf(
            ModelSelector(model) { MAX } bind {
                SampleResponse(
                    model,
                    StopReason.of("bored"),
                    Role.assistant,
                    content
                )
            }
        ))

        val mcp = McpHandler(SseMcpProtocol(metadata, incomingSampling = sampling, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpSampling, McpSampling.Request(listOf(), MaxTokens.of(1)))

            assertNextMessage(
                McpSampling.Response(model, StopReason.of("bored"), Role.assistant, content)
            )
        }
    }

    @Test
    fun `deal with outgoing sampling`() {
        var received: SampleResponse? = null

        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val model = ModelIdentifier.of("name")
        val sampling = OutgoingSampling(listOf(
            clientName bind { received = it }
        ))

        val mcp = McpHandler(SseMcpProtocol(metadata, outgoingSampling = sampling, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            sampling.sample(
                serverName, SampleRequest(
                    listOf(), MaxTokens.of(1), RequestId.of(1),
                    connectRequest = Request(GET, "")
                )
            )

            assertNextMessage(
                McpSampling,
                McpSampling.Request(listOf(), MaxTokens.of(1)),
                RequestId.of(1)
            )
            mcp.sendToMcp(
                McpSampling.Response(model, StopReason.of("bored"), Role.assistant, content),
                RequestId.of(1)
            )

            assertThat(received, equalTo(SampleResponse(model, StopReason.of("bored"), Role.assistant, content)))
        }
    }

    private fun TestSseClient.assertInitializeLoop(mcp: PolyHandler) {
        assertThat(status, equalTo(OK))

        assertThat(
            received().first(),
            equalTo(SseMessage.Event("endpoint", "/message?sessionId=$sessionId"))
        )

        mcp.sendToMcp(
            McpInitialize, McpInitialize.Request(
                VersionedMcpEntity(clientName, Version.of("1")),
                ClientCapabilities(*ProtocolCapability.entries.toTypedArray()), `2024-10-07`
            )
        )

        assertNextMessage(
            McpInitialize.Response(metadata.entity, metadata.capabilities, metadata.protocolVersion)
        )

        mcp.sendToMcp(McpInitialize.Initialized())
    }
}

private fun TestSseClient.assertNextMessage(error: ErrorMessage) {
    assertNextMessage(with(McpJson) { renderError(error, number(1)) })
}

private fun TestSseClient.assertNextMessage(input: McpResponse) {
    assertNextMessage(with(McpJson) { renderResult(asJsonObject(input), number(1)) })
}

private fun TestSseClient.assertNextMessage(notification: McpNotification) {
    assertNextMessage(with(McpJson) { renderNotification(notification) })
}

private fun TestSseClient.assertNextMessage(hasMethod: HasMethod, input: McpRequest, id: Any) {
    assertNextMessage(with(McpJson) {
        renderRequest(
            hasMethod.Method.value,
            asJsonObject(input),
            asJsonObject(id)
        )
    })
}

private fun TestSseClient.assertNextMessage(node: McpNodeType) {
    assertThat(
        received().first(),
        equalTo(SseMessage.Event("message", with(McpJson) { compact(node) }))
    )
}

private fun PolyHandler.sendToMcp(hasMethod: HasMethod, input: ClientMessage.Request) {
    sendToMcp(with(McpJson) {
        compact(renderRequest(hasMethod.Method.value, asJsonObject(input), number(1)))
    })
}

private fun PolyHandler.sendToMcp(hasMethod: ClientMessage.Response, id: Any) {
    sendToMcp(with(McpJson) {
        compact(renderResult(asJsonObject(hasMethod), asJsonObject(id)))
    })
}

private fun PolyHandler.sendToMcp(hasMethod: ClientMessage.Notification) {
    sendToMcp(with(McpJson) {
        compact(renderNotification(hasMethod))
    })
}

private fun PolyHandler.sendToMcp(body: String) {
    assertThat(
        http!!(
            Request(POST, "/message?sessionId=$sessionId").body(body)
        ), hasStatus(ACCEPTED)
    )
}

val sessionId = SessionId.parse("8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e")
