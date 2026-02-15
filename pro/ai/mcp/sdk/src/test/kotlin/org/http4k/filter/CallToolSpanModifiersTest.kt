package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.StatusCode.*
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.junit.jupiter.api.Test

class CallToolSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets request attributes on span`() {
        CallToolSpanModifiers.request(span, asJsonObject(mapOf("name" to "my-tool")))

        assertThat(spanData.attributes.get(stringKey("gen_ai.operation.name")), equalTo("execute_tool"))
        assertThat(spanData.attributes.get(stringKey("gen_ai.tool.name")), equalTo("my-tool"))
    }

    @Test
    fun `sets error attributes on response with isError`() {
        CallToolSpanModifiers.response(span, asJsonObject(mapOf("isError" to true)))

        assertThat(spanData.status.statusCode, equalTo(ERROR))
        assertThat(spanData.attributes.get(stringKey("error.type")), equalTo("tool_error"))
    }
}
