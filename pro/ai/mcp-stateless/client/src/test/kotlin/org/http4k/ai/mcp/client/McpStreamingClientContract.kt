/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference

/**
 * Contract for MCP clients that support streaming capabilities (sampling, elicitation, progress).
 */
abstract class McpStreamingClientContract<T> : McpClientContract<T>() {

    @Test
    fun `can subscribe to resource updates`() {
        val resourceUri = Uri.of("test://resource/1")

        val resources = resources(
            Resource.Static(resourceUri, ResourceName.of("test-resource"), "A test resource") bind {
                ResourceResponse.Ok(listOf(Resource.Content.Text("content", resourceUri)))
            }
        )

        val tools = tools(
            Tool("trigger-update", "triggers a resource update") bind {
                resources.triggerUpdated(resourceUri)
                Ok(Content.Text("triggered"))
            }
        )

        withMcpServer(tools = tools, resources = resources) {
            val latch = CountDownLatch(1)
            val receivedUpdate = AtomicReference<Boolean>(false)

            resources().subscribe(resourceUri) {
                receivedUpdate.set(true)
                latch.countDown()
            }

            tools().call(ToolName.of("trigger-update"), ToolRequest())

            assertThat(latch.await(5, SECONDS), equalTo(true))
            assertThat(receivedUpdate.get(), equalTo(true))

            resources().unsubscribe(resourceUri)
        }
    }
}
