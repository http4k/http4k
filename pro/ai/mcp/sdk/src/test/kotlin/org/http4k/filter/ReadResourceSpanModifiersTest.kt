package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.junit.jupiter.api.Test

class ReadResourceSpanModifiersTest {

    private val span = SdkTracerProvider.builder().build().get("test").spanBuilder("test").startSpan()
    private val spanData get() = (span as ReadableSpan).toSpanData()

    @Test
    fun `sets request attributes on span`() {
        ReadResourceSpanModifiers.request(span, asJsonObject(mapOf("uri" to "file:///test")))

        assertThat(spanData.attributes.get(stringKey("gen_ai.operation.name")), equalTo("read_resource"))
        assertThat(spanData.attributes.get(stringKey("mcp.resource.uri")), equalTo("file:///test"))
    }
}
