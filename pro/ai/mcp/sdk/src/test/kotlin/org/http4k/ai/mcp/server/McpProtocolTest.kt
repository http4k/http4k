package org.http4k.ai.mcp.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Annotations
import org.http4k.ai.mcp.model.Completion
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.CompletionContext
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Priority
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Root
import org.http4k.ai.mcp.model.Size
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.instant
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.model.value
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpNotification
import org.http4k.ai.mcp.protocol.messages.McpPing
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpRequest
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpResponse
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.ServerMessage
import org.http4k.ai.mcp.server.capability.ServerCancellations
import org.http4k.ai.mcp.server.capability.ServerCompletions
import org.http4k.ai.mcp.server.capability.ServerPrompts
import org.http4k.ai.mcp.server.capability.ServerResources
import org.http4k.ai.mcp.server.capability.ServerRoots
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.ServerLogger
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.server.sessions.SessionProvider
import org.http4k.ai.mcp.server.sse.SseMcp
import org.http4k.ai.mcp.server.sse.SseSessions
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.auto
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.Role
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.ai.model.ToolName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.connect.model.MimeType.Companion.IMAGE_GIF
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiString
import org.http4k.format.renderError
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.int
import org.http4k.lens.with
import org.http4k.routing.bind
import org.http4k.security.ResponseType
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.TestSseClient
import org.http4k.testing.assertApproved
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.random.Random

@ExtendWith(JsonApprovalTest::class)
class McpProtocolTest {
    private val serverName = McpEntity.of("server")
    private val clientName = McpEntity.of("server")

    private val metadata = ServerMetaData(
        serverName, Version.of("1"),
        title = "title", instructions = "instructions"
    )

    private val random = Random(0)

    @Test
    fun `performs init loop on startup`() {
        val mcp = SseMcp(
            McpProtocol(
                metadata,
                SseSessions(SessionProvider.Random(Random(0)))
            ),
            NoMcpSecurity
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
            ),
            NoMcpSecurity
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
    fun `handles cancellations`() {
        val cancellations = ServerCancellations()

        val id = McpMessageId.of(123456789)

        var cancelled = false
        cancellations.onCancel { cancelledId, reason, _ ->
            assertThat(cancelledId, equalTo(id))
            assertThat(reason, equalTo("test cancellation"))
            cancelled = true
        }

        val mcp = SseMcp(
            McpProtocol(
                metadata,
                SseSessions(SessionProvider.Random(Random(0))),
                cancellations = cancellations,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpCancelled, McpCancelled.Notification(id, "test cancellation"))
            assertThat(cancelled, equalTo(true))
        }
    }

    @Test
    fun `deal with prompts`() {
        val intArg = Prompt.Arg.int().required("name", "description", mapOf("title" to "title"))
        val icons = listOf(org.http4k.ai.mcp.model.Icon(Uri.of("https://example.com/icon.png")))
        val prompt = Prompt(PromptName.of("prompt"), "description", intArg, title = "title", icons = icons)

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
            ),
            NoMcpSecurity)
        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpPrompt.List, McpPrompt.List.Request())

            assertNextMessage(
                McpPrompt.List.Response(
                    listOf(
                        McpPrompt(
                            PromptName.of("prompt"), "description", "title",
                            listOf(McpPrompt.Argument("name", "description", "title", true)),
                            icons
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
        val icons = listOf(org.http4k.ai.mcp.model.Icon(Uri.of("https://example.com/icon.png")))
        val resource = Resource.Static(
            Uri.of("https://www.http4k.org"), ResourceName.of("HTTP4K"), "description",
            IMAGE_GIF, Size.of(1), Annotations(listOf(Assistant), Priority.of(1.0)), null, icons
        )
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uri)

        val resources = ServerResources(listOf(resource bind { ResourceResponse(listOf(content)) }))

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(Random(0))),
                resources = resources,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpResource.List, McpResource.List.Request())

            assertNextMessage(
                McpResource.List.Response(
                    listOf(
                        McpResource(
                            resource.uri,
                            ResourceName.of("HTTP4K"),
                            "description",
                            IMAGE_GIF,
                            Size.of(1),
                            Annotations(listOf(Assistant), Priority.of(1.0)),
                            null,
                            icons
                        )
                    )
                )
            )

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(resource.uri))

            assertNextMessage(McpResource.Read.Response(listOf(content)))

            mcp.sendToMcp(McpResource.Subscribe, McpResource.Subscribe.Request(resource.uri))

            assertNextMessage(ServerMessage.Response.Empty)

            resources.triggerUpdated(resource.uri)

            assertNextMessage(McpResource.Updated, McpResource.Updated.Notification(resource.uri))

            mcp.sendToMcp(McpResource.Unsubscribe, McpResource.Unsubscribe.Request(resource.uri))

            assertNextMessage(ServerMessage.Response.Empty)

            resources.triggerUpdated(resource.uri)

            assertNoResponse()
        }
    }

    private fun TestSseClient.assertNoResponse() =
        assertThrows<NoSuchElementException> { received().first() }

    @Test
    fun `deal with templated resources`() {
        val icons = listOf(org.http4k.ai.mcp.model.Icon(Uri.of("https://example.com/icon.png")))
        val resource =
            Resource.Templated(
                "https://www.http4k.org/{+template}", "HTTP4K", "description",
                IMAGE_GIF, Size.of(1), Annotations(listOf(Assistant), Priority.of(1.0)), null, icons
            )

        val resources = ServerResources(listOf(resource bind {
            ResourceResponse(
                listOf(
                    Resource.Content.Blob(Base64Blob.encode("image"), it.uri)
                )
            )
        }))
        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                resources = resources,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpResource.List, McpResource.List.Request())

            assertNextMessage(McpResource.List.Response(listOf()))

            mcp.sendToMcp(McpResource.ListTemplates, McpResource.ListTemplates.Request(null))

            assertNextMessage(
                McpResource.ListTemplates.Response(
                    listOf(
                        McpResource(
                            resource.uriTemplate,
                            ResourceName.of("HTTP4K"),
                            "description",
                            IMAGE_GIF,
                            Size.of(1),
                            Annotations(listOf(Assistant), Priority.of(1.0)),
                            null,
                            icons
                        )
                    )
                )
            )

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(Uri.of("https://www.http4k.org/bob")))

            assertNextMessage(
                McpResource.Read.Response(
                    listOf(
                        Resource.Content.Blob(
                            Base64Blob.encode("image"),
                            Uri.of("https://www.http4k.org/bob")
                        )
                    )
                )
            )

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(Uri.of("https://not-http4k/bob")))

            assertNextMessage(InvalidParams)

            mcp.sendToMcp(McpResource.Read, McpResource.Read.Request(Uri.of("otherprotocol://www.http4k.org/bob")))

            assertNextMessage(InvalidParams)
        }
    }

    data class FooBar(val foo: String)

    @Test
    fun `deal with tools`() {
        val stringArg = Tool.Arg.string().required("foo", "description1")
        val intArg = Tool.Arg.int().optional("bar", "description2")
        val output = Tool.Output.auto(FooBar("bar")).toLens()
        val icons = listOf(org.http4k.ai.mcp.model.Icon(Uri.of("https://example.com/icon.png")))

        val unstructuredTool = Tool("unstructured", "description", stringArg, intArg, title = "title", icons = icons)
        val structuredTool = Tool("structured", "description", output = output, title = "title", icons = icons)

        val content =
            Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val tools = ServerTools(
            listOf(
                unstructuredTool bind {
                    val stringArg1 = stringArg(it)
                    val intArg1 = intArg(it)

                    it.meta.progressToken?.let { _ ->
                        it.client.progress(1, 5.0, "d1")
                        it.client.progress(2, 5.0, "d2")
                    }

                    Ok(listOf(content, Content.Text(stringArg1 + intArg1)))
                },
                structuredTool bind { Ok().with(output of FooBar("bar")) })
        )

        val mcp = SseMcp(
            McpProtocol(
                metadata,
                SseSessions(SessionProvider.Random(random)),
                tools = tools,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpTool.List, McpTool.List.Request())

            assertNextMessage(
                McpTool.List.Response(
                    listOf(
                        McpTool(
                            ToolName.of("unstructured"), "description", "title",
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
                            icons
                        ),
                        McpTool(
                            ToolName.of("structured"), "description", "title",
                            mapOf(
                                "type" to "object",
                                "required" to listOf<String>(),
                                "properties" to emptyMap<String, Any>()
                            ),
                            mapOf(
                                "properties" to mapOf(
                                    "foo" to mapOf(
                                        "example" to "bar",
                                        "type" to "string",
                                        "nullable" to false
                                    ),
                                ),
                                "example" to mapOf("foo" to "bar"),
                                "type" to "object",
                                "required" to listOf("foo")
                            ),
                            null,
                            icons
                        )
                    )
                )
            )

            val progressToken = "123"

            mcp.sendToMcp(
                McpTool.Call,
                McpTool.Call.Request(
                    unstructuredTool.name,
                    mapOf("foo" to MoshiString("foo"), "bar" to MoshiInteger(123)), Meta(progressToken)
                )
            )

            assertNextMessage(McpProgress, McpProgress.Notification(progressToken, 1, 5.0, "d1"))
            assertNextMessage(McpProgress, McpProgress.Notification(progressToken, 2, 5.0, "d2"))
            assertNextMessage(McpTool.Call.Response(listOf(content, Content.Text("foo123"))))

            val progress2 = "123"

            mcp.sendToMcp(
                McpTool.Call,
                McpTool.Call.Request(
                    unstructuredTool.name,
                    mapOf("foo" to MoshiString("foo"), "bar" to MoshiString("notAnInt")),
                    Meta(progress2)
                )
            )

            assertNextMessage(InvalidParams)

            mcp.sendToMcp(
                McpTool.Call,
                McpTool.Call.Request(structuredTool.name, mapOf(), Meta(progress2))
            )

            assertNextMessage(
                McpTool.Call.Response(
                    listOf(Content.Text("""{"foo":"bar"}""")),
                    mapOf("foo" to "bar"),
                )
            )

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
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)
            logger.log(Session(firstDeterministicSessionId), McpJson.string("hello"), LogLevel.info, "message")

            assertNoResponse()

            mcp.sendToMcp(McpLogging.SetLevel, McpLogging.SetLevel.Request(LogLevel.debug))

            assertNextMessage(ServerMessage.Response.Empty)

            logger.log(Session(firstDeterministicSessionId), McpJson.string("hello"), LogLevel.info)

            assertNextMessage(
                McpLogging.LoggingMessage,
                McpLogging.LoggingMessage.Notification(McpJson.string("hello"), LogLevel.info)
            )
        }
    }

    @Test
    fun `deal with completions`() {
        val ref = Reference.ResourceTemplate(Uri.of("https://www.http4k.org"))
        val completions = ServerCompletions(
            listOf(ref bind {
                it.meta.progressToken?.let { _ ->
                    it.client.progress(1, 5.0, "d1")
                    it.client.progress(2, 5.0, "d2")
                }

                CompletionResponse(listOf("values"), 1, true)
            })
        )

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                completions = completions,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            val progressToken = "progress"

            mcp.sendToMcp(
                McpCompletion,
                McpCompletion.Request(
                    ref, CompletionArgument("arg", "value"),
                    CompletionContext(mapOf("foo" to "bar")),
                    Meta(progressToken)
                )
            )

            assertNextMessage(McpProgress, McpProgress.Notification(progressToken, 1, 5.0, "d1"))
            assertNextMessage(McpProgress, McpProgress.Notification(progressToken, 2, 5.0, "d2"))

            assertNextMessage(McpCompletion.Response(Completion(listOf("values"), 1, true)))
        }
    }

    @Test
    fun `can handle batched messages`() {
        val ref = Reference.ResourceTemplate(Uri.of("https://www.http4k.org"))
        val completions = ServerCompletions(
            listOf(ref bind { CompletionResponse(listOf("values"), 1, true) })
        )

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                completions = completions,
                random = random
            ),
            NoMcpSecurity
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

    data class Bar(val name: String)
    data class Foo(val foo: Int?, val bar: Bar, val baz: Boolean?)

    @Test
    fun `reports expected tool input schema`(approver: Approver) {
        val stringArg = Tool.Arg.string().required("aString", "description1")
        val intArg = Tool.Arg.int().optional("anInt", "description2")
        val arrayArg = Tool.Arg.int().multi.required("anArray", "description3")
        val enumArg = Tool.Arg.enum<ResponseType>().multi.required("anEnum", "description4")
        val instantArg = Tool.Arg.instant().optional("anInstant", "description5")
        val stringValueArg = Tool.Arg.value(Role).optional("aStringValue", "description6")
        val dateValueArg = Tool.Arg.value(MaxTokens).optional("aIntValue", "description7")
        val objectValueArg = Tool.Arg.auto(Foo(123, Bar("hello"), true)).optional("complexValue", "description8")
        val listObjectValueArg = Tool.Arg.auto(listOf(Bar("hello"))).optional("listArg", "description9")

        val tool = Tool(
            "name",
            "description",
            stringArg,
            intArg,
            arrayArg,
            enumArg,
            instantArg,
            stringValueArg,
            dateValueArg,
            objectValueArg,
            listObjectValueArg,
            execution = org.http4k.ai.mcp.model.ToolExecution(org.http4k.ai.mcp.model.TaskSupport.optional)
        )

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                tools = ServerTools(listOf(tool bind { Ok("") })),
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            with(McpJson) {
                mcp.sendToMcp(renderRequest(McpTool.List, McpTool.List.Request()))

                approver.assertApproved((received().first() as SseMessage.Event).data, APPLICATION_JSON)
            }
        }
    }

    @Test
    fun `can call tool with complex object`(approver: Approver) {
        val example = Foo(123, Bar("hello"), true)

        val objectValueArg = Tool.Arg.auto(example).required("complexValue")
        val listObjectValueArg = Tool.Arg.auto(listOf(Bar("hello"))).required("listArg", "description9")

        val tool = Tool(
            "name",
            "description",
            objectValueArg,
            listObjectValueArg,
            execution = org.http4k.ai.mcp.model.ToolExecution(org.http4k.ai.mcp.model.TaskSupport.required)
        )

        val mcp = SseMcp(
            McpProtocol(
                metadata, SseSessions(SessionProvider.Random(random)),
                tools = ServerTools(listOf(tool bind {
                    Ok(
                        McpJson.asFormatString(
                            mapOf(
                                objectValueArg.meta.name to objectValueArg(it),
                                listObjectValueArg.meta.name to listObjectValueArg(it),
                            )
                        )
                    )
                })),
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            with(McpJson) {
                mcp.sendToMcp(
                    renderRequest(
                        McpTool.Call, McpTool.Call.Request(
                            tool.name,
                            mapOf(
                                objectValueArg.meta.name to asJsonObject(example),
                                listObjectValueArg.meta.name to asJsonObject(listOf(Bar("123"))),
                            )
                        )
                    )
                )
                approver.assertApproved((received().first() as SseMessage.Event).data, APPLICATION_JSON)
            }
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
                ClientCapabilities(), LATEST_VERSION
            )
        )

        assertNextMessage(
            McpInitialize.Response(metadata.entity, metadata.capabilities, LATEST_VERSION, metadata.instructions)
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
        ).status.successful, equalTo(true)
    )

val firstDeterministicSessionId = SessionId.parse("8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e")
