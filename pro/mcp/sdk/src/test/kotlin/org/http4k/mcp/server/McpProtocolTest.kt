package org.http4k.mcp.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MaxTokens
import org.http4k.connect.model.MimeType
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role.Companion.Assistant
import org.http4k.connect.model.StopReason
import org.http4k.connect.model.ToolName
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
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.hamkrest.hasStatus
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.int
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Root
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-11-05`
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpLogging
import org.http4k.mcp.protocol.messages.McpNotification
import org.http4k.mcp.protocol.messages.McpPing
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpRequest
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpResponse
import org.http4k.mcp.protocol.messages.McpRoot
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.protocol.messages.ServerMessage
import org.http4k.mcp.server.capability.ServerCompletions
import org.http4k.mcp.server.capability.ServerPrompts
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerRoots
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.http.HttpStreamingMcp
import org.http4k.mcp.server.protocol.ClientRequestTarget.Entity
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.ServerLogger
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.sessions.SessionProvider
import org.http4k.mcp.server.sse.SseMcp
import org.http4k.mcp.server.sse.SseSessions
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.routing.bind
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.random.Random

class McpProtocolTest {
    private val serverName = McpEntity.of("server")
    private val clientName = McpEntity.of("server")

    private val metadata = ServerMetaData(serverName, Version.of("1"))

    private val random = Random(0)

    @Test
    fun `performs init loop on startup`() {
        val mcp = SseMcp(
            McpProtocol(
                metadata,
                SseSessions(SessionProvider.Random(Random(0)))
            )
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpPing, McpPing.Request)

            assertNextMessage(ServerMessage.Response.Empty)
        }
    }

    @Test
    fun `update roots`() {
        val roots = ServerRoots()

        val mcp = SseMcp(
            McpProtocol(
                metadata,
                SseSessions(SessionProvider.Random(Random(0))),
                roots = roots,
                random = random
            )
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpRoot.Changed, McpRoot.Changed.Notification)

            assertNextMessage(McpRoot.List, McpRoot.List.Request(), McpMessageId.of(7425097216252813))

            val newRoots = listOf(Root(Uri.of("asd"), "name"))

            mcp.sendToMcp(McpRoot.List.Response(newRoots), McpMessageId.of(7425097216252813))

            assertThat(roots.toList(), equalTo(newRoots))
        }
    }

    @Test
    fun `deal with prompts`() {
        val intArg = Prompt.Arg.int().required("name", "description")
        val prompt = Prompt(PromptName.of("prompt"), "description", intArg)

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(Random(0))),
                prompts = ServerPrompts(
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
                ),
                random = random
            ))
        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpPrompt.List, McpPrompt.List.Request())

            assertNextMessage(
                McpPrompt.List.Response(
                    listOf(
                        McpPrompt(
                            PromptName.of("prompt"), "description",
                            listOf(McpPrompt.Argument("name", "description", true))
                        )
                    )
                )
            )

            mcp.sendToMcp(McpPrompt.Get, McpPrompt.Get.Request(prompt.name, mapOf("name" to "123")))

            assertNextMessage(
                McpPrompt.Get.Response(
                    listOf(Message(Assistant, Content.Text("321"))),
                    "description"
                )
            )

            mcp.sendToMcp(McpPrompt.Get, McpPrompt.Get.Request(prompt.name, mapOf("name" to "notAnInt")))

            assertNextMessage(InvalidParams)
        }
    }

    @Test
    fun `deal with static resources`() {
        val resource = Resource.Static(Uri.of("https://www.http4k.org"), ResourceName.of("HTTP4K"), "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uri)

        val resources = ServerResources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(Random(0))),
                resources = resources,
                random = random
            )
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpResource.List, McpResource.List.Request())

            assertNextMessage(
                McpResource.List.Response(
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

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(resource.uri))

            assertNextMessage(McpResource.Read.Response(listOf(content)))

            mcp.sendToMcp(McpResource.Subscribe, McpResource.Subscribe.Request(resource.uri))

            resources.triggerUpdated(resource.uri)

            assertNextMessage(McpResource.Updated, McpResource.Updated.Notification(resource.uri))

            mcp.sendToMcp(McpResource.Unsubscribe, McpResource.Unsubscribe.Request(resource.uri))

            resources.triggerUpdated(resource.uri)

            assertNoResponse()
        }
    }

    private fun TestSseClient.assertNoResponse() =
        assertThrows<NoSuchElementException> { received().first() }

    @Test
    fun `deal with templated resources`() {
        val resource =
            Resource.Templated(Uri.of("https://www.http4k.org/{+template}"), ResourceName.of("HTTP4K"), "description")
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uriTemplate)

        val resources = ServerResources(listOf(resource bind { ResourceResponse(listOf(content)) }))
        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                resources = resources,
                random = random
            )
        )

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
                            ResourceName.of("HTTP4K"),
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

        val tools = ServerTools(listOf(tool bind {
            val stringArg1 = stringArg(it)
            val intArg1 = intArg(it)

            it.meta.progress?.let { p ->
                it.client.progress(1, 5.0)
                it.client.progress(2, 5.0)
            }

            ToolResponse.Ok(listOf(content, Content.Text(stringArg1 + intArg1)))
        }))

        val mcp = SseMcp(
            McpProtocol(
                metadata,
                SseSessions(SessionProvider.Random(random)),
                tools = tools,
                random = random
            )
        )

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

            val progressToken = "123"

            mcp.sendToMcp(
                McpTool.Call,
                McpTool.Call.Request(
                    tool.name,
                    mapOf("foo" to MoshiString("foo"), "bar" to MoshiInteger(123)), Meta(progressToken)
                )
            )


            assertNextMessage(McpProgress, McpProgress.Notification(1, 5.0, progressToken))
            assertNextMessage(McpProgress, McpProgress.Notification(2, 5.0, progressToken))
            assertNextMessage(McpTool.Call.Response(listOf(content, Content.Text("foo123"))))

            val progress2 = "123"

            mcp.sendToMcp(
                McpTool.Call,
                McpTool.Call.Request(
                    tool.name,
                    mapOf("foo" to MoshiString("foo"), "bar" to MoshiString("notAnInt")),
                    Meta(progress2)
                )
            )

            assertNextMessage(InvalidParams)

            tools.items = emptyList()

            assertNextMessage(McpTool.List.Changed, McpTool.List.Changed.Notification)
        }
    }

    @Test
    fun `deal with logger`() {
        val logger = ServerLogger()
        val mcp = SseMcp(
            McpProtocol(
                metadata,
                SseSessions(SessionProvider.Random(random)),
                logger = logger,
                random = random
            )
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)
            logger.log(Session(firstDeterministicSessionId), LogLevel.info, "message", emptyMap())

            assertNoResponse()

            mcp.sendToMcp(McpLogging.SetLevel, McpLogging.SetLevel.Request(LogLevel.debug))

            logger.log(Session(firstDeterministicSessionId), LogLevel.info, "message", emptyMap())

            assertNextMessage(
                McpLogging.LoggingMessage,
                McpLogging.LoggingMessage.Notification(LogLevel.info, "message", emptyMap())
            )
        }
    }

    @Test
    fun `deal with completions`() {
        val ref = Reference.Resource(Uri.of("https://www.http4k.org"))
        val completions = ServerCompletions(
            listOf(ref bind {
                it.meta.progress?.let { p ->
                    it.client.progress(1, 5.0)
                    it.client.progress(2, 5.0)
                }

                CompletionResponse(listOf("values"), 1, true)
            })
        )

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                completions = completions,
                random = random
            )
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            val progressToken = "progress"

            mcp.sendToMcp(
                McpCompletion,
                McpCompletion.Request(ref, CompletionArgument("arg", "value"), Meta(progressToken))
            )

            assertNextMessage(McpProgress, McpProgress.Notification(1, 5.0, progressToken))
            assertNextMessage(McpProgress, McpProgress.Notification(2, 5.0, progressToken))

            assertNextMessage(McpCompletion.Response(Completion(listOf("values"), 1, true)))
        }
    }

    @Test
    fun `can handle batched messages`() {
        val ref = Reference.Resource(Uri.of("https://www.http4k.org"))
        val completions = ServerCompletions(
            listOf(ref bind { CompletionResponse(listOf("values"), 1, true) })
        )

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                completions = completions,
                random = random
            )
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            with(McpJson) {
                mcp.sendToMcp(
                    array(
                        listOf(
                            renderRequest(McpPrompt.List, McpPrompt.List.Request()),
                            renderRequest(McpResource.List, McpResource.List.Request()),
                            renderRequest(McpTool.List, McpTool.List.Request()),
                        )
                    )
                )
            }

            assertNextMessage(McpPrompt.List.Response(listOf()))
            assertNextMessage(McpResource.List.Response(listOf()))
            assertNextMessage(McpTool.List.Response(listOf()))
        }
    }

    @Test
    @Disabled
    fun `deal with client sampling`() {
        val content = Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val model = ModelName.of("name")
        val sampling = ServerSampling(Random(0))

        val mcp = HttpStreamingMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(Random(0))),
            )
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {

            assertInitializeLoop(mcp)

            val received = sampling.sampleClient(
                Entity(metadata.entity.name),
                SamplingRequest(listOf(), MaxTokens.of(1)),
                Duration.ofSeconds(5)
            )

            assertNextMessage(
                McpSampling,
                McpSampling.Request(listOf(), MaxTokens.of(1)),
                McpMessageId.of(7425097216252813)
            )

            mcp.sendToMcp(
                McpSampling.Response(model, null, Assistant, content),
                McpMessageId.of(7425097216252813)
            )

            mcp.sendToMcp(
                McpSampling.Response(model, StopReason.of("bored"), Assistant, content),
                McpMessageId.of(7425097216252813)
            )

            // this is ignored!
            mcp.sendToMcp(
                McpSampling.Response(model, StopReason.of("another stop reason"), Assistant, content),
                McpMessageId.of(7425097216252813)
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

    private fun TestSseClient.assertInitializeLoop(mcp: PolyHandler) {
        assertThat(status, equalTo(OK))

        assertThat(
            received().first(),
            equalTo(SseMessage.Event("endpoint", "/message?sessionId=$firstDeterministicSessionId"))
        )

        mcp.sendToMcp(
            McpInitialize, McpInitialize.Request(
                VersionedMcpEntity(clientName, Version.of("1")),
                ClientCapabilities(), `2024-11-05`
            )
        )

        assertNextMessage(
            McpInitialize.Response(metadata.entity, metadata.capabilities, `2024-11-05`)
        )

        mcp.sendToMcp(McpInitialize.Initialized, McpInitialize.Initialized.Notification)
    }

    private fun TestSseClient.assertNextMessage(error: ErrorMessage) {
        assertNextMessage(with(McpJson) { renderError(error, number(1)) })
    }

    private fun TestSseClient.assertNextMessage(input: McpResponse) {
        assertNextMessage(with(McpJson) { renderResult(asJsonObject(input), number(1)) })
    }

    private fun TestSseClient.assertNextMessage(hasMethod: McpRpc, notification: McpNotification) {
        assertNextMessage(with(McpJson) {
            renderRequest(
                hasMethod.Method.value,
                asJsonObject(notification),
                nullNode()
            )
        })
    }

    private fun TestSseClient.assertNextMessage(hasMethod: McpRpc, input: McpRequest, id: Any) {
        assertNextMessage(with(McpJson) {
            renderRequest(
                hasMethod.Method.value,
                asJsonObject(input),
                asJsonObject(id)
            )
        })
    }

    private var inboundCounter = 1

    private fun TestSseClient.assertNextMessage(node: McpNodeType) {
        assertThat(
            received().first(),
            equalTo(
                SseMessage.Event(
                    "message",
                    with(McpJson) { compact(node) },
                    SseEventId(inboundCounter++.toString())
                )
            )
        )
    }

}

private fun PolyHandler.sendToMcp(hasMethod: McpRpc, input: ClientMessage.Request) =
    sendToMcp(with(McpJson) { renderRequest(hasMethod, input) })

fun McpJson.renderRequest(hasMethod: McpRpc, input: ClientMessage.Request, id: Int = 1) =
    renderRequest(hasMethod.Method.value, asJsonObject(input), number(id))

private fun PolyHandler.sendToMcp(hasMethod: ClientMessage.Response, id: Any) =
    sendToMcp(with(McpJson) {
        renderResult(asJsonObject(hasMethod), asJsonObject(id))
    })

var outboundMessageCounter = 0
private fun PolyHandler.sendToMcp(hasMethod: McpRpc, input: ClientMessage.Notification) =
    sendToMcp(with(McpJson) {
        renderRequest(hasMethod.Method.value, asJsonObject(input), number(outboundMessageCounter++))
    })

private fun PolyHandler.sendToMcp(body: McpNodeType) =
    assertThat(
        http!!(
            Request(POST, "/message?sessionId=$firstDeterministicSessionId").body(McpJson.compact(body))
        ), hasStatus(ACCEPTED)
    )

val firstDeterministicSessionId = SessionId.parse("8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e")
