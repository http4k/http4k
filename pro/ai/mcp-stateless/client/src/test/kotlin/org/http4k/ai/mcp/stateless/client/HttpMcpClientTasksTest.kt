/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.client.http.HttpMcpClient
import org.http4k.ai.mcp.stateless.model.Content.Text
import org.http4k.ai.mcp.stateless.model.TaskStatus.completed
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.protocol.ClientCapabilities
import org.http4k.ai.mcp.stateless.protocol.ClientProtocolCapability.ElicitationForm
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.server.capability.then
import org.http4k.ai.mcp.stateless.server.extension.McpTasks
import org.http4k.ai.mcp.stateless.server.extension.TASKS_EXTENSION
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.http4k.routing.stateless.bind
import org.http4k.routing.stateless.mcp
import org.junit.jupiter.api.Test

class HttpMcpClientTasksTest {

    private val tasks = McpTasks()

    private val server = mcp(
        ServerMetaData("tasked", "1.0.0"),
        NoMcpSecurity,
        tasks.asTask().then(Tool("slow", "takes a while") bind { ToolResponse.Ok("eventually") }),
        extensions = listOf(tasks)
    )

    private fun client(declaresTasks: Boolean) = HttpMcpClient(
        Uri.of("/mcp"),
        http = server.http!!,
        capabilities = ClientCapabilities(ElicitationForm)
            .let { if (declaresTasks) it.withExtensions(TASKS_EXTENSION to emptyMap<String, Any>()) else it }
    )

    @Test
    fun `a declared client is handed a task and polls it to completion`() {
        client(declaresTasks = true).use { client ->
            val handle = client.tools().call(ToolName.of("slow")).valueOrNull() as ToolResponse.Task

            val polled = generateSequence { client.tasks().get(handle.task.taskId).valueOrNull() }
                .first { it!!.status == completed }!!

            assertThat(polled.taskId, equalTo(handle.task.taskId))
        }
    }

    @Test
    fun `a client which has not declared tasks is never handed one`() {
        client(declaresTasks = false).use { client ->
            val response = client.tools().call(ToolName.of("slow")).valueOrNull()

            assertThat((response as ToolResponse.Ok).content, equalTo(listOf(Text("eventually"))))
        }
    }
}
