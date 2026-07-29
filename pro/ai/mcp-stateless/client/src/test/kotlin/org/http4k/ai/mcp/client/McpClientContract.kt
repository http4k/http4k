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
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.coerce
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Message
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
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.lens.with
import org.http4k.routing.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

abstract class McpClientContract : PortBasedTest {

    val clientName get() = McpEntity.of("foobar")

    fun withMcpServer(
        tools: Tools = tools(),
        resources: Resources = resources(),
        prompts: Prompts = prompts(),
        completions: Completions = completions(),
        test: McpClient.() -> Unit
    ) {
        val metaData = ServerMetaData(McpEntity.of("David"), Version.of("0.0.1"))
        val protocol = McpProtocol(
            metaData.entity,
            tools = tools,
            resources = resources,
            prompts = prompts,
            completions = completions
        )

        val server = toHandler(protocol).asServer(Helidon(0)).start()
        val mcpClient = clientFor(server.port())

        try {
            mcpClient.start()
            mcpClient.test()
        } finally {
            mcpClient.stop()
            server.stop()
        }
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

    abstract fun toHandler(protocol: McpProtocol): PolyHandler

    abstract fun clientFor(port: Int): McpClient
}
