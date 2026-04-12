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
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.junit.jupiter.api.Test

class CompletionSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets operation name`() {
        CompletionSpanModifiers.request(span, asJsonObject(mapOf("ref" to mapOf("type" to "ref/prompt", "name" to "my-prompt"))))

        assertThat(spanData.attributes.get(stringKey("gen_ai.operation.name")), equalTo("complete"))
    }

    @Test
    fun `sets completion ref from prompt name`() {
        CompletionSpanModifiers.request(span, asJsonObject(mapOf("ref" to mapOf("type" to "ref/prompt", "name" to "my-prompt"))))

        assertThat(spanData.attributes.get(stringKey("mcp.completion.ref")), equalTo("my-prompt"))
    }

    @Test
    fun `sets completion ref from resource uri`() {
        CompletionSpanModifiers.request(span, asJsonObject(mapOf("ref" to mapOf("type" to "ref/resource", "uri" to "docs://articles/{+topic}"))))

        assertThat(spanData.attributes.get(stringKey("mcp.completion.ref")), equalTo("docs://articles/{+topic}"))
    }
}
