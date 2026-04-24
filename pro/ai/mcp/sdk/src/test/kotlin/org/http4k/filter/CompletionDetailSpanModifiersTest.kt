/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4k.ai.mcp.model.Completion
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class CompletionDetailSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets arguments from request`(approver: Approver) {
        val request = McpCompletion.Request(
            McpCompletion.Request.Params(
                ref = Reference.Prompt("my-prompt"),
                argument = CompletionArgument("city", "Lon")
            ),
            id = 1
        )
        CompletionDetailSpanModifiers(span, request)

        approver.assertApproved(spanData.attributes.get(stringKey("gen_ai.completion.arguments"))!!, APPLICATION_JSON)
    }

    @Test
    fun `sets result from response`(approver: Approver) {
        val response = McpCompletion.Response(
            McpCompletion.Response.Result(Completion("London", "Los Angeles")),
            id = 1
        )
        CompletionDetailSpanModifiers(span, response)

        approver.assertApproved(spanData.attributes.get(stringKey("gen_ai.completion.result"))!!, APPLICATION_JSON)
    }
}
