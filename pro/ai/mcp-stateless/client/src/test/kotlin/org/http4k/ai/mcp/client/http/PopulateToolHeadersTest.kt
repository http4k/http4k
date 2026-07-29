/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.model.ToolName
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.http4k.lens.MCP_NAME
import org.junit.jupiter.api.Test

class PopulateToolHeadersTest {

    private val toolsCall = McpRpcMethod.of("tools/call")

    private val toolWithHeaders = McpTool(
        name = ToolName.of("execute_sql"),
        description = "Execute SQL",
        title = null,
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "region" to mapOf("type" to "string", "x-mcp-header" to "Region"),
                "priority" to mapOf("type" to "number", "x-mcp-header" to "Priority"),
                "query" to mapOf("type" to "string")
            )
        ),
        outputSchema = null,
        annotations = null,
    )

    private val toolWithoutHeaders = McpTool(
        name = ToolName.of("simple_tool"),
        description = "Simple",
        title = null,
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "input" to mapOf("type" to "string")
            )
        ),
        outputSchema = null,
        annotations = null,
    )

    @Test
    fun `sets Mcp-Method and Mcp-Name headers`() {
        var captured: Request? = null
        val filter = PopulateToolHeaders(listOf(toolWithoutHeaders), toolsCall, ToolName.of("simple_tool"), emptyMap())
        filter { captured = it; Response(OK) }(Request(POST, "/mcp"))

        assertThat(Header.MCP_METHOD(captured!!), equalTo(toolsCall))
        assertThat(Header.MCP_NAME(captured), equalTo("simple_tool"))
    }

    @Test
    fun `adds Mcp-Param headers for annotated properties`() {
        var captured: Request? = null
        val filter = PopulateToolHeaders(
            listOf(toolWithHeaders), toolsCall,
            ToolName.of("execute_sql"),
            mapOf("region" to "us-west1", "priority" to 42, "query" to "SELECT 1")
        )
        filter { captured = it; Response(OK) }(Request(POST, "/mcp"))

        assertThat(captured!!.header("Mcp-Param-Region"), equalTo("us-west1"))
        assertThat(captured.header("Mcp-Param-Priority"), equalTo("42"))
        assertThat(captured.header("Mcp-Param-query"), absent())
    }

    @Test
    fun `omits header when argument is absent`() {
        var captured: Request? = null
        val filter = PopulateToolHeaders(
            listOf(toolWithHeaders), toolsCall,
            ToolName.of("execute_sql"),
            mapOf("query" to "SELECT 1")
        )
        filter { captured = it; Response(OK) }(Request(POST, "/mcp"))

        assertThat(captured!!.header("Mcp-Param-Region"), absent())
        assertThat(captured.header("Mcp-Param-Priority"), absent())
    }

    @Test
    fun `no extra headers when tool has no annotations`() {
        var captured: Request? = null
        val filter = PopulateToolHeaders(
            listOf(toolWithoutHeaders), toolsCall,
            ToolName.of("simple_tool"),
            mapOf("input" to "hello")
        )
        filter { captured = it; Response(OK) }(Request(POST, "/mcp"))

        assertThat(captured!!.headers.none { it.first.startsWith("Mcp-Param-") }, equalTo(true))
    }

    @Test
    fun `no extra headers when tool not found in list`() {
        var captured: Request? = null
        val filter = PopulateToolHeaders(
            emptyList(), toolsCall,
            ToolName.of("unknown"),
            mapOf("region" to "us-west1")
        )
        filter { captured = it; Response(OK) }(Request(POST, "/mcp"))

        assertThat(captured!!.headers.none { it.first.startsWith("Mcp-Param-") }, equalTo(true))
    }

    @Test
    fun `boolean values encoded as strings`() {
        val tool = McpTool(
            name = ToolName.of("bool_tool"),
            description = "Bool tool",
            title = null,
            inputSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "verbose" to mapOf("type" to "boolean", "x-mcp-header" to "Verbose")
                )
            ),
            outputSchema = null,
            annotations = null,
        )
        var captured: Request? = null
        val filter = PopulateToolHeaders(listOf(tool), toolsCall, ToolName.of("bool_tool"), mapOf("verbose" to true))
        filter { captured = it; Response(OK) }(Request(POST, "/mcp"))

        assertThat(captured!!.header("Mcp-Param-Verbose"), equalTo("true"))
    }
}
