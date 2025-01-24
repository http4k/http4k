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
import org.http4k.format.renderNotification
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.int
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SampleResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.Root
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ClientMessage
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.McpCompletion
import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.McpInitialize
import org.http4k.mcp.protocol.McpLogging
import org.http4k.mcp.protocol.McpNotification
import org.http4k.mcp.protocol.McpPing
import org.http4k.mcp.protocol.McpPrompt
import org.http4k.mcp.protocol.McpRequest
import org.http4k.mcp.protocol.McpResource
import org.http4k.mcp.protocol.McpResponse
import org.http4k.mcp.protocol.McpRoot
import org.http4k.mcp.protocol.McpSampling
import org.http4k.mcp.protocol.McpTool
import org.http4k.mcp.protocol.MessageId
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-10-07`
import org.http4k.mcp.protocol.ServerMessage
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.Version
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

    private val metadata = ServerMetaData(McpEntity("server", Version.of("1")))

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

            assertNextMessage(McpRoot.List, McpRoot.List.Request(), MessageId.of(6079152038928072179))

            val newRoots = listOf(Root(Uri.of("asd"), "name"))

            mcp.sendToMcp(McpRoot.List.Response(newRoots), MessageId.of(6079152038928072179))

            assertThat(roots.toList(), equalTo(newRoots))
        }
    }

    @Test
    fun `deal with prompts`() {
        val prompt = Prompt("prompt", "description", listOf(Prompt.Argument("name", "description", true)))

        val mcp = McpHandler(SseMcpProtocol(metadata, prompts = Prompts(
            listOf(
                prompt bind {
                    PromptResponse(
                        "description",
                        listOf(
                            Message(Role.assistant, Content.Text(it.input["name"]!!.reversed()))
                        )
                    )
                }
            )
        ), random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpPrompt.List, McpPrompt.List.Request())

            assertNextMessage(McpPrompt.List.Response(listOf(prompt)))

            mcp.sendToMcp(McpPrompt.Get, McpPrompt.Get.Request(prompt.name, mapOf("name" to "value")))

            assertNextMessage(
                McpPrompt.Get.Response(
                    listOf(Message(Role.assistant, Content.Text("eulav"))),
                    "description"
                )
            )
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

            assertNextMessage(McpResource.List.Response(listOf(McpResource(resource.uri, null, "HTTP4K", "description", null))))

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

            assertNextMessage(McpResource.Template.List.Response(listOf(McpResource(null, resource.uriTemplate, "HTTP4K", "description", null))))

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(resource.uriTemplate))

            assertNextMessage(McpResource.Read.Response(listOf(content)))
        }
    }

    @Test
    fun `deal with tools`() {
        val tool = Tool(
            "name", "description",
            Tool.Arg.required("foo", "description1"),
            Tool.Arg.int().optional("bar", "description2")
        )

        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val tools = Tools(listOf(tool bind { ToolResponse.Ok(listOf(content)) }))
        val mcp = McpHandler(SseMcpProtocol(metadata, tools = tools, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpTool.List, McpTool.List.Request())

            assertNextMessage(
                McpTool.List.Response(
                    listOf(
                        McpTool(
                            "name", "description",
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
                McpTool.Call.Request(tool.name, mapOf("foo" to "foo", "bar" to "bar"))
            )

            assertNextMessage(McpTool.Call.Response(listOf(content)))

            tools.items = emptyList()

            assertNextMessage(McpTool.List.Changed)
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
    fun `deal with sampling`() {
        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val model = ModelIdentifier.of("name")
        val sampling = Sampling(listOf(
            ModelSelector(model) { 1 } bind {
                SampleResponse(
                    model,
                    StopReason.of("bored"),
                    Role.assistant,
                    content
                )
            }
        ))

        val mcp = McpHandler(SseMcpProtocol(metadata, sampling = sampling, random = Random(0)))

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpSampling, McpSampling.Request(listOf(), MaxTokens.of(1)))

            assertNextMessage(
                McpSampling.Response(model, StopReason.of("bored"), Role.assistant, content)
            )
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
                McpEntity("client", Version.of("1")),
                ClientCapabilities(), `2024-10-07`
            )
        )

        assertNextMessage(
            McpInitialize.Response(metadata.entity, metadata.capabilities, metadata.protocolVersion)
        )

        mcp.sendToMcp(McpInitialize.Initialized())
    }
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
