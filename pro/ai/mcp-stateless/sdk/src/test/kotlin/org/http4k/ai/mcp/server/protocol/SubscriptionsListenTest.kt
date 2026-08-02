/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpSubscriptions
import org.http4k.ai.mcp.protocol.messages.SubscriptionFilter
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.lens.protocolVersion
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.http.HttpMcp
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.http4k.lens.accept
import org.http4k.routing.bind
import org.http4k.sse.SseMessage.Event
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test

class SubscriptionsListenTest {

    private val tools = tools(Tool("greet", "greets") bind { ToolResponse.Ok("hi") })
    private val prompts = prompts(Prompt(PromptName.of("p"), "d") bind { PromptResponse.Ok(Assistant, "hi") })
    private val resources = resources()
    private val server = HttpMcp(
        McpProtocol(VersionedMcpEntity("subs-server", "1.0.0"), tools = tools, prompts = prompts, resources = resources),
        NoMcpSecurity
    )

    // stateless requests must self-describe via reserved _meta or they're rejected -32602 (A2)
    private val requestMeta = MetaKey.clientCapabilities().toLens()(
        ClientCapabilities(), MetaKey.protocolVersion().toLens()(LATEST_VERSION, Meta.default)
    )

    private fun listenRequest(id: Any?, filter: SubscriptionFilter) =
        McpSubscriptions.Listen.Request(McpSubscriptions.Listen.Request.Params(filter, requestMeta), id).let { message ->
            Request(POST, "/mcp")
                .accept(TEXT_EVENT_STREAM)
                .with(Header.MCP_METHOD of message.method)
                .body(McpJson.asFormatString(message))
        }

    @Test
    fun `listen sends the acknowledgement first, tagged with the subscriptionId`() {
        val client = server.testSseClient(listenRequest("42", SubscriptionFilter(toolsListChanged = true)))

        val ack = client.received().first() as Event

        assertThat(ack.event, equalTo("message"))
        assertThat(ack.data, containsSubstring("notifications/subscriptions/acknowledged"))
        assertThat(ack.data, containsSubstring("\"io.modelcontextprotocol/subscriptionId\":\"42\""))
        assertThat(ack.data, containsSubstring("\"toolsListChanged\":true"))
    }

    @Test
    fun `reassigning items pushes a list_changed notification tagged with the subscriptionId`() {
        val messages = server.testSseClient(listenRequest("7", SubscriptionFilter(toolsListChanged = true)))
            .received().iterator()

        messages.next() // ack

        tools.items = tools.items.toList()

        val changed = messages.next() as Event
        assertThat(changed.data, containsSubstring("notifications/tools/list_changed"))
        assertThat(changed.data, containsSubstring("\"io.modelcontextprotocol/subscriptionId\":\"7\""))
    }

    @Test
    fun `resourceSubscriptions pushes resources_updated only for subscribed URIs`() {
        val messages = server.testSseClient(
            listenRequest("3", SubscriptionFilter(resourceSubscriptions = listOf("res://watched")))
        ).received().iterator()

        messages.next() // ack

        resources.triggerUpdated(Uri.of("res://ignored")) // not subscribed -> nothing
        resources.triggerUpdated(Uri.of("res://watched")) // subscribed -> notification

        val updated = messages.next() as Event
        assertThat(updated.data, containsSubstring("notifications/resources/updated"))
        assertThat(updated.data, containsSubstring("res://watched"))
        assertThat(updated.data, containsSubstring("\"io.modelcontextprotocol/subscriptionId\":\"3\""))
    }

    @Test
    fun `unrequested change types are never sent`() {
        val messages = server.testSseClient(listenRequest("9", SubscriptionFilter(promptsListChanged = true)))
            .received().iterator()

        messages.next() // ack

        tools.items = tools.items.toList() // NOT subscribed -> must produce nothing
        prompts.items = prompts.items.toList() // subscribed -> produces a notification

        // if the tools change had wrongly been sent, this next message would be tools, not prompts
        val next = messages.next() as Event
        assertThat(next.data, containsSubstring("notifications/prompts/list_changed"))
    }
}
