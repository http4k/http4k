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
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class CompletionSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets operation name`() {
        CompletionSpanModifiers(span, McpCompletion.Request(
            McpCompletion.Request.Params(ref = Reference.Prompt("my-prompt"), argument = CompletionArgument("arg", "val")),
            id = 1
        ))

        assertThat(spanData.attributes.get(stringKey("gen_ai.operation.name")), equalTo("complete"))
    }

    @Test
    fun `sets completion ref from prompt name`() {
        CompletionSpanModifiers(span, McpCompletion.Request(
            McpCompletion.Request.Params(ref = Reference.Prompt("my-prompt"), argument = CompletionArgument("arg", "val")),
            id = 1
        ))

        assertThat(spanData.attributes.get(stringKey("mcp.completion.ref")), equalTo("my-prompt"))
    }

    @Test
    fun `sets completion ref from resource uri`() {
        CompletionSpanModifiers(span, McpCompletion.Request(
            McpCompletion.Request.Params(ref = Reference.ResourceTemplate(Uri.of("docs://articles/{+topic}")), argument = CompletionArgument("arg", "val")),
            id = 1
        ))

        assertThat(spanData.attributes.get(stringKey("mcp.completion.ref")), equalTo("docs://articles/{+topic}"))
    }
}
