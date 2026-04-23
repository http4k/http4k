/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.model.ToolName
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.format.renderResult
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class CallToolDetailSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets arguments from request`(approver: Approver) {
        val request = McpTool.Call.Request.Params(ToolName.of("my-tool"), mapOf("city" to McpJson.string("London")))
        CallToolDetailSpanModifiers.request(span, asJsonObject(request))

        approver.assertApproved(spanData.attributes.get(stringKey("gen_ai.tool.call.arguments"))!!, APPLICATION_JSON)
    }

    @Test
    fun `sets result from response`(approver: Approver) {
        val response = McpTool.Call.Response.Result(content = listOf(Content.Text("hello")))
        CallToolDetailSpanModifiers.response(span, McpJson.run { renderResult(asJsonObject(response), number(1)) })

        approver.assertApproved(spanData.attributes.get(stringKey("gen_ai.tool.call.result"))!!, APPLICATION_JSON)
    }

    @Test
    fun `no result attribute when response has no content`() {
        val response = McpTool.Call.Response.Result()
        CallToolDetailSpanModifiers.response(span, McpJson.run { renderResult(asJsonObject(response), number(1)) })

        assertThat(spanData.attributes.get(stringKey("gen_ai.tool.call.result")), equalTo(null))
    }
}
