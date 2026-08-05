/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.http.HttpMcpClient
import org.http4k.ai.mcp.conformance.server.McpConformanceServer
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class McpConformanceServerTest {

    private val client = HttpMcpClient(Uri.of("/mcp"), http = McpConformanceServer().http!!)

    @Test
    fun `assembles and exposes the conformance tools over the stateless transport`() {
        val tools = client.tools().list().valueOrNull()?.map { it.name.value }?.toSet()

        assertThat(
            tools, equalTo(
                setOf(
                    "test_simple_text", "test_image_content", "test_audio_content", "test_embedded_resource",
                    "test_multiple_content_types", "test_tool_with_progress", "test_error_handling",
                    "test_tool_with_logging",
                    "test_input_required_result_elicitation", "test_input_required_result_request_state",
                    "test_input_required_result_multi_round", "test_input_required_result_capabilities",
                    "test_input_required_result_tampered_state",
                    "test_missing_capability", "test_streaming_elicitation", "test_logging_tool",
                    "test_trigger_tool_change", "test_trigger_prompt_change"
                )
            )
        )
    }
}
