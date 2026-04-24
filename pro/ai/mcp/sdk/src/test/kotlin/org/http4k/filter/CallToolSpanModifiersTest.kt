/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.model.ToolName
import org.junit.jupiter.api.Test

class CallToolSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets request attributes on span`() {
        CallToolSpanModifiers(span, McpTool.Call.Request(McpTool.Call.Request.Params(ToolName.of("my-tool")), id = 1))

        assertThat(spanData.attributes.get(stringKey("gen_ai.operation.name")), equalTo("execute_tool"))
        assertThat(spanData.attributes.get(stringKey("gen_ai.tool.name")), equalTo("my-tool"))
    }

    @Test
    fun `sets error attributes on response with isError`() {
        CallToolSpanModifiers(span, McpTool.Call.Response(McpTool.Call.Response.Result(isError = true), id = 1))

        assertThat(spanData.status.statusCode, equalTo(ERROR))
        assertThat(spanData.attributes.get(stringKey("error.type")), equalTo("tool_error"))
    }
}
