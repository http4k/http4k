/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter.stateless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4k.ai.mcp.stateless.model.PromptName
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.junit.jupiter.api.Test

class GetPromptDetailSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `each prompt argument becomes its own variable attribute`() {
        val request = McpPrompt.Get.Request(
            McpPrompt.Get.Request.Params(PromptName.of("my-prompt"), mapOf("city" to "London", "language" to "French")),
            id = 1
        )
        GetPromptDetailSpanModifiers(span, request.asMcpRequest())

        assertThat(spanData.attributes.get(stringKey("gen_ai.prompt.variable.city")), equalTo("London"))
        assertThat(spanData.attributes.get(stringKey("gen_ai.prompt.variable.language")), equalTo("French"))
        assertThat(spanData.attributes.get(stringKey("gen_ai.prompt.arguments")), equalTo(null))
    }
}
