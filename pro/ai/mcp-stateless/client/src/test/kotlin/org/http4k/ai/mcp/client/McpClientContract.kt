/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.coerce
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.LogLevel.info
import org.http4k.ai.mcp.model.LogMessage
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.capability.completions
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.protocol.Completions
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.Prompts
import org.http4k.ai.mcp.server.protocol.Resources
import org.http4k.ai.mcp.server.protocol.Tools
import org.http4k.ai.mcp.util.McpJson.auto
import org.http4k.ai.mcp.util.McpJson.obj
import org.http4k.ai.mcp.util.McpJson.string
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.http4k.lens.with
import org.http4k.routing.bind
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS

abstract class McpClientContract : PortBasedTest {

    val clientName get() = McpEntity.of("foobar")

    open val streamsSubscriptions: Boolean get() = true

    fun withMcpServer(
        tools: Tools = tools(),
        resources: Resources = resources(),
        prompts: Prompts = prompts(),
        completions: Completions = completions(),
        test: McpClient.() -> Unit
    ) {
        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            tools = tools,
            resources = resources,
            prompts = prompts,
            completions = completions
        )

        withClient(protocol, test)
    }

    data class FooBar(val foo: String)

    @Test
    fun `can list and get prompts`() {
        val prompts = prompts(
            Prompt(PromptName.of("prompt"), "description1") bind {
                PromptResponse.Ok(listOf(Message(Assistant, Content.Text(it.toString()))), "description")
            }
        )

        withMcpServer(prompts = prompts) {
            assertThat(prompts().list().coerce<List<McpPrompt>>().size, equalTo(1))
            assertThat(
                prompts().get(PromptName.of("prompt"), PromptRequest(mapOf("a1" to "foo")))
                    .coerce<PromptResponse.Ok>().description,
                equalTo("description")
            )
        }
    }

    @Test
    fun `can list and read resources`() {
        val resources = resources(
            Resource.Static(Uri.of("https://http4k.org"), ResourceName.of("HTTP4K"), "description") bind {
                ResourceResponse.Ok(listOf(Resource.Content.Text("foo", Uri.of(""))))
            },
            Resource.Templated(ResourceUriTemplate.of("https://http4k.org"), ResourceName.of("HTTP4K"), "templated resource") bind {
                ResourceResponse.Ok(listOf(Resource.Content.Text("foo", Uri.of(""))))
            }
        )

        withMcpServer(resources = resources) {
            assertThat(resources().list().coerce<List<McpResource>>().size, equalTo(1))
            assertThat(resources().listTemplates().coerce<List<McpResource>>().size, equalTo(1))
            assertThat(
                resources().read(ResourceRequest(Uri.of("https://http4k.org"))).coerce<ResourceResponse.Ok>(),
                equalTo(ResourceResponse.Ok(listOf(Resource.Content.Text("foo", Uri.of("")))))
            )
        }
    }

    @Test
    fun `can complete references`() {
        val completions = completions(
            Reference.ResourceTemplate(Uri.of("https://http4k.org")) bind { CompletionResponse.Ok(listOf("1", "2")) }
        )

        withMcpServer(completions = completions) {
            assertThat(
                completions().complete(
                    Reference.ResourceTemplate(Uri.of("https://http4k.org")),
                    CompletionRequest(CompletionArgument("foo", "bar"))
                ).coerce<CompletionResponse.Ok>(),
                equalTo(CompletionResponse.Ok(listOf("1", "2")))
            )
        }
    }

    @Test
    fun `can list and call tools`() {
        val toolArg = Tool.Arg.string().required("name")
        val output = Tool.Output.auto(FooBar("bar")).toLens()

        val tools = tools(
            Tool("reverse", "description", toolArg) bind {
                ToolResponse.Ok(listOf(Content.Text(toolArg(it).reversed())))
            },
            Tool("reverseStructured", "description", toolArg) bind {
                ToolResponse.Ok().with(output of FooBar(toolArg(it).reversed()))
            },
        )

        withMcpServer(tools = tools) {
            assertThat(tools().list().coerce<List<McpTool>>().size, equalTo(2))
            assertThat(
                tools().call(ToolName.of("reverse"), ToolRequest().with(toolArg of "foobar")).coerce<ToolResponse.Ok>(),
                equalTo(ToolResponse.Ok(listOf(Content.Text("raboof"))))
            )
            assertThat(
                tools().call(ToolName.of("reverseStructured"), ToolRequest().with(toolArg of "foobar")).coerce<ToolResponse.Ok>(),
                equalTo(ToolResponse.Ok(listOf(Content.Text("""{"foo":"raboof"}""")), obj("foo" to string("raboof"))))
            )
        }
    }

    @Test
    fun `tool can return error response`() {
        val toolArg = Tool.Arg.string().required("name")
        val tools = tools(Tool("failing", "description", toolArg) bind { ToolResponse.Error("oh no") })

        withMcpServer(tools = tools) {
            val actual = tools().call(ToolName.of("failing"), ToolRequest().with(toolArg of "boom")).valueOrNull()
            assertThat(actual, present(isA<ToolResponse.Error>()))
        }
    }

    @Test
    fun `can discover the server`() {
        withMcpServer {
            val info = discover().coerce<VersionedMcpEntity>()
            assertThat(info.name, equalTo(McpEntity.of("David")))
            assertThat(info.version, equalTo(Version.of("0.0.1")))
        }
    }

    @Test
    fun `streams progress and log to the per-call callbacks, then the result returns`() {
        val tools = tools(
            Tool("greet", "greets") bind { req ->
                req.client.progress("t", 1, 2.0, "step1")
                req.client.progress("t", 2, 2.0, "step2")
                req.client.log("hello", info)
                ToolResponse.Ok("done")
            }
        )

        withMcpServer(tools = tools) {
            val progresses = mutableListOf<Progress>()
            val logs = mutableListOf<LogMessage>()

            val result = tools().call(ToolName.of("greet"), onProgress = { progresses += it }, onLog = { logs += it })

            assertThat((result.valueOrNull() as ToolResponse.Ok).content, equalTo(listOf(Content.Text("done"))))
            assertThat(progresses.map { it.progress }, equalTo(listOf(1, 2)))
            assertThat(logs.map { it.level }, equalTo(listOf(info)))
        }
    }

    @Test
    fun `drives a tool-call input-required round-trip`() {
        val arg = Tool.Arg.string().required("name")
        val tools = tools(
            Tool("greet", "greets", arg) bind { req ->
                when (req.inputResponses["login"]) {
                    is ElicitationResponse.Ok -> ToolResponse.Ok("hi ${arg(req)} [state=${req.requestState}]")

                    else -> ToolResponse.InputRequired(
                        inputRequests = mapOf("login" to ElicitationRequest.Form("please log in")), requestState = "s1"
                    )
                }
            }
        )

        withMcpServer(tools = tools) {
            val request = ToolRequest().with(arg of "bob")

            val first = tools().call(ToolName.of("greet"), request).valueOrNull() as ToolResponse.InputRequired
            assertThat(first.inputRequests, equalTo(mapOf("login" to ElicitationRequest.Form("please log in"))))
            assertThat(first.requestState, equalTo("s1"))

            val retried = tools().call(
                ToolName.of("greet"),
                request.copy(inputResponses = login, requestState = first.requestState)
            )
            assertThat((retried.valueOrNull() as ToolResponse.Ok).content, equalTo(listOf(Content.Text("hi bob [state=s1]"))))
        }
    }

    @Test
    fun `drives a prompts-get input-required round-trip`() {
        val prompts = prompts(
            Prompt(PromptName.of("greet"), "greets") bind { req ->
                when (req.inputResponses["login"]) {
                    is ElicitationResponse.Ok -> PromptResponse.Ok(Assistant, "hi [state=${req.requestState}]")

                    else -> PromptResponse.InputRequired(
                        inputRequests = mapOf("login" to ElicitationRequest.Form("please log in")), requestState = "s2"
                    )
                }
            }
        )

        withMcpServer(prompts = prompts) {
            val first = prompts().get(PromptName.of("greet"), PromptRequest()).valueOrNull() as PromptResponse.InputRequired
            assertThat(first.requestState, equalTo("s2"))

            val retried = prompts().get(
                PromptName.of("greet"), PromptRequest(inputResponses = login, requestState = first.requestState)
            )
            assertThat(
                (retried.valueOrNull() as PromptResponse.Ok).messages,
                equalTo(listOf(Message(Assistant, Content.Text("hi [state=s2]"))))
            )
        }
    }

    @Test
    fun `drives a resources-read input-required round-trip`() {
        val resources = resources(
            Resource.Static(Uri.of("res://greet"), ResourceName.of("greet")) bind { req ->
                when (req.inputResponses["login"]) {
                    is ElicitationResponse.Ok -> ResourceResponse.Ok(listOf(Resource.Content.Text("hi [state=${req.requestState}]", req.uri)))

                    else -> ResourceResponse.InputRequired(
                        inputRequests = mapOf("login" to ElicitationRequest.Form("please log in")), requestState = "s3"
                    )
                }
            }
        )

        withMcpServer(resources = resources) {
            val uri = Uri.of("res://greet")

            val first = resources().read(ResourceRequest(uri)).valueOrNull() as ResourceResponse.InputRequired
            assertThat(first.requestState, equalTo("s3"))

            val retried = resources().read(
                ResourceRequest(uri, inputResponses = login, requestState = first.requestState)
            )
            assertThat(
                retried.valueOrNull() as ResourceResponse.Ok,
                equalTo(ResourceResponse.Ok(listOf(Resource.Content.Text("hi [state=s3]", uri))))
            )
        }
    }

    @Test
    fun `a tools list change fires the onListChanged handler`() {
        assumeTrue(streamsSubscriptions)
        val tools = tools(Tool("greet", "greets") bind { ToolResponse.Ok("hi") })

        withMcpServer(tools = tools) {
            val fired = CountDownLatch(1)
            val sub = tools().onListChanged { fired.countDown() }.valueOrNull()!!
            try {
                assertThat(awaitFiring(fired) { tools.items = tools.items.toList() }, equalTo(true))
            } finally {
                sub.close()
            }
        }
    }

    @Test
    fun `a prompts list change fires the onListChanged handler`() {
        assumeTrue(streamsSubscriptions)
        val prompts = prompts(Prompt(PromptName.of("p"), "d") bind { PromptResponse.Ok(Assistant, "hi") })

        withMcpServer(prompts = prompts) {
            val fired = CountDownLatch(1)
            val sub = prompts().onListChanged { fired.countDown() }.valueOrNull()!!
            try {
                assertThat(awaitFiring(fired) { prompts.items = prompts.items.toList() }, equalTo(true))
            } finally {
                sub.close()
            }
        }
    }

    @Test
    fun `a resources list change fires the onListChanged handler`() {
        assumeTrue(streamsSubscriptions)
        val resources = resources(
            Resource.Static(Uri.of("res://r"), ResourceName.of("r"), "d") bind {
                ResourceResponse.Ok(listOf(Resource.Content.Text("x", Uri.of("res://r"))))
            }
        )

        withMcpServer(resources = resources) {
            val fired = CountDownLatch(1)
            val sub = resources().onListChanged { fired.countDown() }.valueOrNull()!!
            try {
                assertThat(awaitFiring(fired) { resources.items = resources.items.toList() }, equalTo(true))
            } finally {
                sub.close()
            }
        }
    }

    @Test
    fun `a resource update fires the subscribe handler for that URI only`() {
        assumeTrue(streamsSubscriptions)
        val resources = resources(
            Resource.Static(Uri.of("res://watched"), ResourceName.of("w"), "d") bind {
                ResourceResponse.Ok(listOf(Resource.Content.Text("x", Uri.of("res://watched"))))
            }
        )

        withMcpServer(resources = resources) {
            val watched = CountDownLatch(1)
            val sub = resources().subscribe(Uri.of("res://watched")) { watched.countDown() }.valueOrNull()!!
            try {
                assertThat(
                    awaitFiring(watched) {
                        resources.triggerUpdated(Uri.of("res://ignored")) // no handler for this URI
                        resources.triggerUpdated(Uri.of("res://watched"))
                    },
                    equalTo(true)
                )
            } finally {
                sub.close()
            }
        }
    }

    private fun awaitFiring(latch: CountDownLatch, trigger: () -> Unit): Boolean {
        repeat(50) {
            trigger()
            if (latch.await(100, MILLISECONDS)) return true
        }
        return false
    }

    private val login = mapOf("login" to ElicitationResponse.Ok(ElicitationAction.accept))

    abstract fun withClient(protocol: McpProtocol, test: McpClient.() -> Unit)
}
