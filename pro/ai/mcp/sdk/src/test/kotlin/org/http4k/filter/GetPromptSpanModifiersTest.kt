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
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.junit.jupiter.api.Test

class GetPromptSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets request attributes on span`() {
        GetPromptSpanModifiers(span, McpPrompt.Get.Request(
            McpPrompt.Get.Request.Params(PromptName.of("my-prompt")),
            id = 1
        ).asMcpRequest())

        assertThat(spanData.attributes.get(stringKey("gen_ai.operation.name")), equalTo("get_prompt"))
        assertThat(spanData.attributes.get(stringKey("gen_ai.prompt.name")), equalTo("my-prompt"))
    }
}
