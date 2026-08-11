package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent
import org.junit.jupiter.api.Test

/**
 * The SCF runtime reads the JSON emitted by this event, so its wire format is a contract. These
 * expectations were captured from scf-java-events 0.0.4 running against fastjson 1.2.73, before that
 * dependency was removed.
 */
class APIGatewayProxyResponseEventTest {

    @Test
    fun `renders populated response`() {
        assertThat(
            APIGatewayProxyResponseEvent().apply {
                statusCode = 200
                body = "hello there"
                headers = mapOf("a" to "b", "Content-Type" to "text/plain")
            }.toString(),
            equalTo("""{"headers":{"a":"b","Content-Type":"text/plain"},"body":"hello there","statusCode":200}""")
        )
    }

    @Test
    fun `omits unset fields`() {
        assertThat(APIGatewayProxyResponseEvent().toString(), equalTo("{}"))
    }

    @Test
    fun `renders base64 flag and empty values`() {
        assertThat(
            APIGatewayProxyResponseEvent().apply {
                statusCode = 404
                isBase64Encoded = true
                body = ""
                headers = emptyMap()
            }.toString(),
            equalTo("""{"isBase64Encoded":true,"headers":{},"body":"","statusCode":404}""")
        )
    }

    @Test
    fun `drops null valued headers`() {
        assertThat(
            APIGatewayProxyResponseEvent().apply {
                statusCode = 200
                headers = mapOf("a" to "b", "nullValued" to null, "z" to "y")
            }.toString(),
            equalTo("""{"headers":{"a":"b","z":"y"},"statusCode":200}""")
        )
    }

    @Test
    fun `escapes quotes and backslashes in bodies and headers`() {
        assertThat(
            APIGatewayProxyResponseEvent().apply {
                body = "quote \" backslash \\ done"
                headers = mapOf("x-\"weird\"" to "v\nv")
            }.toString(),
            equalTo("""{"headers":{"x-\"weird\"":"v\nv"},"body":"quote \" backslash \\ done"}""")
        )
    }

    @Test
    fun `uses the named escapes for the whitespace controls`() {
        assertThat(
            APIGatewayProxyResponseEvent().apply { body = charsOf(0x08, 0x09, 0x0A, 0x0C, 0x0D) }.toString(),
            equalTo("""{"body":"\b\t\n\f\r"}""")
        )
    }

    @Test
    fun `escapes remaining controls, the c1 range and the line separators as uppercase hex`() {
        assertThat(
            APIGatewayProxyResponseEvent().apply { body = charsOf(0x01, 0x0B, 0x1F, 0x7F, 0x85, 0x2028, 0x2029) }
                .toString(),
            equalTo("{\"body\":\"\\u0001\\u000B\\u001F\\u007F\\u0085\\u2028\\u2029\"}")
        )
    }

    @Test
    fun `passes through printable ascii and other non-ascii untouched`() {
        assertThat(
            APIGatewayProxyResponseEvent().apply { body = "slash / lt < unicode é 中" }.toString(),
            equalTo("""{"body":"slash / lt < unicode é 中"}""")
        )
    }

    @Test
    fun `equality covers every field`() {
        fun event() = APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            body = "b"
            headers = mapOf("a" to "b")
            isBase64Encoded = true
        }

        assertThat(event(), equalTo(event()))
        assertThat(event().hashCode(), equalTo(event().hashCode()))
        assertThat(event() == event().apply { statusCode = 201 }, equalTo(false))
        assertThat(event() == event().apply { headers = emptyMap() }, equalTo(false))
        assertThat(event() == event().apply { body = "other" }, equalTo(false))
    }
}

private fun charsOf(vararg codes: Int) = codes.map(::Char).joinToString("")
