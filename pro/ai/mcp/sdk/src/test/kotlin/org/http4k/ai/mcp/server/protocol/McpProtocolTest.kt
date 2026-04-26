/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

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
import org.http4k.ai.mcp.model.Icon
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
import org.http4k.ai.mcp.model.TaskSupport.optional
import org.http4k.ai.mcp.model.TaskSupport.required
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.ToolExecution
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
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcEmptyResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpPing
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.capability.SimpleInitializeHandler
import org.http4k.ai.mcp.server.capability.cancellations
import org.http4k.ai.mcp.server.capability.completions
import org.http4k.ai.mcp.server.capability.initializer
import org.http4k.ai.mcp.server.capability.logger
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.roots
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.server.sessions.SessionProvider
import org.http4k.ai.mcp.server.sse.SseMcp
import org.http4k.ai.mcp.server.sse.SseSessions
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.auto
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
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.MetaKey
import org.http4k.lens.int
import org.http4k.lens.progressToken
import org.http4k.lens.with
import org.http4k.routing.bind
import org.http4k.security.ResponseType
import org.http4k.sse.SseMessage
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.TestSseClient
import org.http4k.testing.assertApproved
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
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

            mcp.sendToMcp(McpPing.Request(McpPing.Request.Params(), 1))

            assertNextMessage(McpJsonRpcEmptyResponse(1))
        }
    }

    @Test
    fun `update roots`() {
        val roots = roots()

        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(Random(0))),
                initializer(SimpleInitializeHandler(metadata)),
                roots = roots,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpRoot.Changed.Notification())

            assertNextMessage(McpRoot.List.Request(McpRoot.List.Request.Params(), 7425097216252813))

            val newRoots = listOf(Root(Uri.of("asd"), "name"))

            mcp.sendToMcp(McpRoot.List.Response(McpRoot.List.Response.Result(newRoots), 7425097216252813))

            assertThat(roots.toList(), equalTo(newRoots))
        }
    }

    @Test
    fun `handles cancellations`() {
        val cancellations = cancellations()

        val id = McpMessageId.of(123456789)

        var cancelled = false
        cancellations.onCancel { cancelledId, reason, _ ->
            assertThat(cancelledId, equalTo(id))
            assertThat(reason, equalTo("test cancellation"))
            cancelled = true
        }

        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(Random(0))),
                initializer(SimpleInitializeHandler(metadata)),
                cancellations = cancellations,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpCancelled.Notification(McpCancelled.Notification.Params(id, "test cancellation")))
            assertThat(cancelled, equalTo(true))
        }
    }

    @Test
    fun `deal with prompts`() {
        val intArg = Prompt.Arg.int().required("name", "description", mapOf("title" to "title"))
        val icons = listOf(Icon(Uri.of("https://example.com/icon.png")))
        val prompt = Prompt(PromptName.of("prompt"), "description", intArg, title = "title", icons = icons)

        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(Random(0))),
                initializer(SimpleInitializeHandler(metadata)),
                prompts = prompts(
                    listOf(
                        prompt bind {
                            PromptResponse.Ok(
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

            mcp.sendToMcp(McpPrompt.List.Request(McpPrompt.List.Request.Params(), 1))

            assertNextMessage(
                McpPrompt.List.Response(
                    McpPrompt.List.Response.Result(
                        listOf(
                            McpPrompt(
                                PromptName.of("prompt"), "description", "title",
                                listOf(McpPrompt.Argument("name", "description", "title", true)),
                                icons
                            )
                        )
                    ), 1
                )
            )

            mcp.sendToMcp(McpPrompt.Get.Request(McpPrompt.Get.Request.Params(prompt.name, mapOf("name" to "123")), 1))

            assertNextMessage(
                McpPrompt.Get.Response(
                    McpPrompt.Get.Response.Result(
                        listOf(Message(Assistant, Content.Text("321"))),
                        "description"
                    ), 1
                )
            )

            mcp.sendToMcp(McpPrompt.Get.Request(McpPrompt.Get.Request.Params(prompt.name, mapOf("name" to "notAnInt")), 1))

            assertNextMessage(McpJsonRpcErrorResponse(1, InvalidParams))
        }
    }

    @Test
    fun `deal with static resources`() {
        val icons = listOf(Icon(Uri.of("https://example.com/icon.png")))
        val resource = Resource.Static(
            Uri.of("https://www.http4k.org"), ResourceName.of("HTTP4K"), "description",
            IMAGE_GIF, Size.of(1), Annotations(listOf(Assistant), Priority.of(1.0)), null, icons
        )
        val content = Resource.Content.Blob(Base64Blob.encode("image"), resource.uri)

        val res = resources(listOf(resource bind { ResourceResponse.Ok(listOf(content)) }))

        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(Random(0))),
                initializer(SimpleInitializeHandler(metadata)),
                resources = res,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpResource.List.Request(McpResource.List.Request.Params(), 1))

            assertNextMessage(
                McpResource.List.Response(
                    McpResource.List.Response.Result(
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
                    ), 1
                )
            )

            mcp.sendToMcp(McpResource.Read.Request(McpResource.Read.Request.Params(resource.uri), 1))

            assertNextMessage(McpResource.Read.Response(McpResource.Read.Response.Result(listOf(content)), 1))

            mcp.sendToMcp(McpResource.Subscribe.Request(McpResource.Subscribe.Request.Params(resource.uri), 1))

            assertNextMessage(McpJsonRpcEmptyResponse(1))

            res.triggerUpdated(resource.uri)

            assertNextMessage(McpResource.Updated.Notification(McpResource.Updated.Notification.Params(resource.uri)))

            mcp.sendToMcp(McpResource.Unsubscribe.Request(McpResource.Unsubscribe.Request.Params(resource.uri), 1))

            assertNextMessage(McpJsonRpcEmptyResponse(1))

            res.triggerUpdated(resource.uri)

        }
    }

    @Test
    fun `deal with templated resources`() {
        val icons = listOf(Icon(Uri.of("https://example.com/icon.png")))
        val resource =
            Resource.Templated(
                "https://www.http4k.org/{+template}", "HTTP4K", "description",
                IMAGE_GIF, Size.of(1), Annotations(listOf(Assistant), Priority.of(1.0)), null, icons
            )

        val res = resources(listOf(resource bind {
            ResourceResponse.Ok(
                listOf(
                    Resource.Content.Blob(Base64Blob.encode("image"), it.uri)
                )
            )
        }))
        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(Random(0))),
                initializer(SimpleInitializeHandler(metadata)),
                resources = res,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpResource.List.Request(McpResource.List.Request.Params(), 1))

            assertNextMessage(McpResource.List.Response(McpResource.List.Response.Result(listOf()), 1))

            mcp.sendToMcp(McpResource.ListTemplates.Request(McpResource.ListTemplates.Request.Params(null), 1))

            assertNextMessage(
                McpResource.ListTemplates.Response(
                    McpResource.ListTemplates.Response.Result(
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
                    ), 1
                )
            )

            mcp.sendToMcp(McpResource.Read.Request(McpResource.Read.Request.Params(Uri.of("https://www.http4k.org/bob")), 1))

            assertNextMessage(
                McpResource.Read.Response(
                    McpResource.Read.Response.Result(
                        listOf(
                            Resource.Content.Blob(
                                Base64Blob.encode("image"),
                                Uri.of("https://www.http4k.org/bob")
                            )
                        )
                    ), 1
                )
            )

            mcp.sendToMcp(McpResource.Read.Request(McpResource.Read.Request.Params(Uri.of("https://not-http4k/bob")), 1))

            assertNextMessage(McpJsonRpcErrorResponse(1, InvalidParams))

            mcp.sendToMcp(McpResource.Read.Request(McpResource.Read.Request.Params(Uri.of("otherprotocol://www.http4k.org/bob")), 1))

            assertNextMessage(McpJsonRpcErrorResponse(1, InvalidParams))
        }
    }

    data class FooBar(val foo: String)

    @Test
    fun `deal with tools`() {
        val stringArg = Tool.Arg.string().required("foo", "description1")
        val intArg = Tool.Arg.int().optional("bar", "description2")
        val output = Tool.Output.auto(FooBar("bar")).toLens()
        val icons = listOf(Icon(Uri.of("https://example.com/icon.png")))

        val unstructuredTool = Tool("unstructured", "description", stringArg, intArg, title = "title", icons = icons)
        val structuredTool = Tool("structured", "description", output = output, title = "title", icons = icons)

        val content =
            Content.Image(Base64Blob.encode("image"), MimeType.of(APPLICATION_FORM_URLENCODED))

        val tools = tools(
                unstructuredTool bind {
                    val stringArg1 = stringArg(it)
                    val intArg1 = intArg(it)

                    MetaKey.progressToken<String>().toLens()(it.meta)?.let { p ->
                        it.client.progress(p, 1, 5.0, "d1")
                        it.client.progress(p, 2, 5.0, "d2")
                    }

                    Ok(listOf(content, Content.Text(stringArg1 + intArg1)))
                },
            structuredTool bind { Ok().with(output of FooBar("bar")) }
        )

        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(random)),
                initializer(SimpleInitializeHandler(metadata)),
                tools = tools,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpTool.List.Request(McpTool.List.Request.Params(), 1))

            assertNextMessage(
                McpTool.List.Response(
                    McpTool.List.Response.Result(
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
                    ), 1
                )
            )

            val progressToken = "123"

            mcp.sendToMcp(
                McpTool.Call.Request(
                    McpTool.Call.Request.Params(
                        unstructuredTool.name,
                        mapOf("foo" to MoshiString("foo"), "bar" to MoshiInteger(123)), Meta(MetaKey.progressToken<String>().toLens() of progressToken)
                    ), 1
                )
            )

            assertNextMessage(McpProgress.Notification(McpProgress.Notification.Params(progressToken, 1, 5.0, "d1")))
            assertNextMessage(McpProgress.Notification(McpProgress.Notification.Params(progressToken, 2, 5.0, "d2")))
            assertNextMessage(McpTool.Call.Response(McpTool.Call.Response.Result(listOf(content, Content.Text("foo123"))), 1))

            val progress2 = "123"

            mcp.sendToMcp(
                McpTool.Call.Request(
                    McpTool.Call.Request.Params(
                        unstructuredTool.name,
                        mapOf("foo" to MoshiString("foo"), "bar" to MoshiString("notAnInt")),
                        Meta(MetaKey.progressToken<String>().toLens() of progress2)
                    ), 1
                )
            )

            assertNextMessage(McpJsonRpcErrorResponse(1, InvalidParams))

            mcp.sendToMcp(
                McpTool.Call.Request(
                    McpTool.Call.Request.Params(structuredTool.name, mapOf(), Meta(MetaKey.progressToken<String>().toLens() of progress2)), 1
                )
            )

            assertNextMessage(
                McpTool.Call.Response(
                    McpTool.Call.Response.Result(
                        listOf(Content.Text("""{"foo":"bar"}""")),
                        mapOf("foo" to "bar"),
                    ), 1
                )
            )

            tools.items = emptyList()

            assertNextMessage(McpTool.List.Changed.Notification(McpTool.List.Changed.Notification.Params()))
        }
    }

    @Test
    fun `deal with logger`() {
        val logger = logger()
        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(random)),
                initializer(SimpleInitializeHandler(metadata)),
                logger = logger,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)
            logger.log(Session(firstDeterministicSessionId), McpJson.string("hello"), LogLevel.info, "message")

            mcp.sendToMcp(McpLogging.SetLevel.Request(McpLogging.SetLevel.Request.Params(LogLevel.debug), 1))

            assertNextMessage(McpJsonRpcEmptyResponse(1))

            logger.log(Session(firstDeterministicSessionId), McpJson.string("hello"), LogLevel.info)

            assertNextMessage(
                McpLogging.LoggingMessage.Notification(
                    McpLogging.LoggingMessage.Notification.Params(McpJson.string("hello"), LogLevel.info)
                )
            )
        }
    }

    @Test
    fun `deal with completions`() {
        val ref = Reference.ResourceTemplate(Uri.of("https://www.http4k.org"))
        val completions = completions(
            ref bind {
                MetaKey.progressToken<String>().toLens()(it.meta)?.let { p ->
                    it.client.progress(p, 1, 5.0, "d1")
                    it.client.progress(p, 2, 5.0, "d2")
                }

                CompletionResponse.Ok(listOf("values"), 1, true)
            }
        )

        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(random)),
                initializer(SimpleInitializeHandler(metadata)),
                completions = completions,
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            val progressToken = "progress"

            mcp.sendToMcp(
                McpCompletion.Request(
                    McpCompletion.Request.Params(
                        ref, CompletionArgument("arg", "value"),
                        CompletionContext(mapOf("foo" to "bar")),
                        Meta(MetaKey.progressToken<String>().toLens() of progressToken)
                    ), 1
                )
            )

            assertNextMessage(McpProgress.Notification(McpProgress.Notification.Params(progressToken, 1, 5.0, "d1")))
            assertNextMessage(McpProgress.Notification(McpProgress.Notification.Params(progressToken, 2, 5.0, "d2")))

            assertNextMessage(McpCompletion.Response(McpCompletion.Response.Result(Completion(listOf("values"), 1, true)), 1))
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
        val output = Tool.Output.auto(Foo(123, Bar("hello"), true)).toLens()

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
            output = output,
            execution = ToolExecution(optional)
        )

        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(random)),
                initializer(SimpleInitializeHandler(metadata)),
                tools = tools(listOf(tool bind { Ok().with(output of Foo(123, Bar(""), true)) })),
                random = random
            ),
            NoMcpSecurity
        )

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpTool.List.Request(McpTool.List.Request.Params(), 1))

            approver.assertApproved((received().first() as SseMessage.Event).data, APPLICATION_JSON)
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
            execution = ToolExecution(required)
        )

        val mcp = SseMcp(
            McpProtocol(
                SseSessions(SessionProvider.Random(random)),
                initializer(SimpleInitializeHandler(metadata)),
                tools = tools(listOf(tool bind {
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

            mcp.sendToMcp(
                McpTool.Call.Request(
                    McpTool.Call.Request.Params(
                        tool.name,
                        mapOf(
                            objectValueArg.meta.name to McpJson.asJsonObject(example),
                            listObjectValueArg.meta.name to McpJson.asJsonObject(listOf(Bar("123"))),
                        )
                    ), 1
                )
            )
            val message = received().first() as SseMessage.Event
            approver.assertApproved((message).data, APPLICATION_JSON)
        }
    }

    private fun TestSseClient.assertInitializeLoop(mcp: PolyHandler) {
        assertThat(status, equalTo(OK))

        assertThat(
            received().first(),
            equalTo(SseMessage.Event("endpoint", "/message?sessionId=$firstDeterministicSessionId"))
        )

        mcp.sendToMcp(
            McpInitialize.Request(
                McpInitialize.Request.Params(
                    VersionedMcpEntity(clientName, Version.of("1")),
                    ClientCapabilities(), LATEST_VERSION
                ), 1
            )
        )

        assertNextMessage(
            McpInitialize.Response(
                McpInitialize.Response.Result(metadata.entity, metadata.capabilities, LATEST_VERSION, metadata.instructions), 1
            )
        )

        mcp.sendToMcp(McpInitialize.Initialized.Notification())
    }

    private fun TestSseClient.assertNextMessage(input: McpJsonRpcMessage) {
        val received = received().first() as SseMessage.Event
        val expectedData = McpJson.asFormatString(input)
        assertThat(
            with(McpJson) { parse(received.data) },
            equalTo(with(McpJson) { parse(expectedData) })
        )
    }
}

private fun PolyHandler.sendToMcp(input: McpJsonRpcMessage) =
    assertThat(
        http!!(
            Request(POST, "/message?sessionId=$firstDeterministicSessionId")
                .body(McpJson.asFormatString(input))
        ).status.successful, equalTo(true)
    )

val firstDeterministicSessionId = SessionId.parse("8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e")
