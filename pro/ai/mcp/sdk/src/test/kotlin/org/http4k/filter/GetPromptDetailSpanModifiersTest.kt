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
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.model.Role
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class GetPromptDetailSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets arguments from request`(approver: Approver) {
        val request = McpPrompt.Get.Request(
            McpPrompt.Get.Request.Params(PromptName.of("my-prompt"), mapOf("city" to "London")),
            id = 1
        )
        GetPromptDetailSpanModifiers(span, request)

        approver.assertApproved(spanData.attributes.get(stringKey("gen_ai.prompt.arguments"))!!, APPLICATION_JSON)
    }

    @Test
    fun `sets result from response`(approver: Approver) {
        val response = McpPrompt.Get.Response(
            McpPrompt.Get.Response.Result(listOf(Message(Role.Assistant, Content.Text("hello world")))),
            id = 1
        )
        GetPromptDetailSpanModifiers(span, response)

        approver.assertApproved(spanData.attributes.get(stringKey("gen_ai.prompt.result"))!!, APPLICATION_JSON)
    }

    @Test
    fun `no result attribute when response has no messages`() {
        val response = McpPrompt.Get.Response(
            McpPrompt.Get.Response.Result(emptyList()),
            id = 1
        )
        GetPromptDetailSpanModifiers(span, response)

        // empty list is still serialized, so attribute should be set
        assertThat(spanData.attributes.get(stringKey("gen_ai.prompt.result"))!!.contains("[]"), equalTo(true))
    }
}
