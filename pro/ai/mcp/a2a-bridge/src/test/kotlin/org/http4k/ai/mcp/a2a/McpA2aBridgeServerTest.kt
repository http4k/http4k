/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.a2a

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import dev.forkhandles.result4k.Success
import org.http4k.ai.a2a.model.A2ARole.ROLE_AGENT
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.Version
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.mcp.testing.useClient
import org.http4k.ai.model.ToolName
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.a2aJsonRpc
import org.http4k.routing.mcpA2aBridgeServer
import org.http4k.testing.toHttpHandler
import org.junit.jupiter.api.Test
import java.util.Random
import org.http4k.ai.mcp.protocol.Version as McpVersion

class McpA2aBridgeServerTest {

    private val card = AgentCard(
        name = "Test Agent",
        version = Version.of("1.0.0"),
        description = "agent for tests"
    )

    private val random = Random(0)

    @Test
    fun `wires an MCP server that forwards the inbound Authorization header to the A2A agent`() {
        var receivedAuth: String? = null
        val a2a = a2aJsonRpc(
            agentCard = card,
            tasks = TaskStorage.InMemory(),
            pushNotifications = PushNotificationConfigStorage.InMemory(),
            messageHandler = {
                receivedAuth = it.http.header("Authorization")
                Message(MessageId.of("m1"), ROLE_AGENT, listOf(Part.Text("ok")))
            }
        )

        val server = mcpA2aBridgeServer(
            identity = ServerMetaData(McpEntity.of("bridge"), McpVersion.of("1.0.0")),
            baseUri = Uri.of(""),
            security = NoMcpSecurity,
            random = random,
            http = a2a.toHttpHandler()
        )

        server.testMcpClient(Request(POST, "/mcp").header("Authorization", "Bearer xyz")).useClient {
            start()
            val response = tools().call(ToolName.of("send_message"), ToolRequest(args = mapOf("message" to "hi")))
            assertThat(response, isA<Success<ToolResponse.Ok>>())
        }

        assertThat(receivedAuth, equalTo("Bearer xyz"))
    }
}
