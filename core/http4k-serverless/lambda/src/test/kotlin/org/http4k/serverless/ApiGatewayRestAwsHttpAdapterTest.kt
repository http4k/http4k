package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ApiGatewayRestAwsHttpAdapterTest {

    @Test
    fun `converts into http4k request`() {
        val request = mapOf(
            "path" to "/path",
            "queryStringParameters" to mapOf("query" to "value"),
            "body" to "input body",
            "headers" to mapOf("c" to "d"),
            "isBase64Encoded" to false,
            "httpMethod" to "GET"
        )

        assertThat(
            ApiGatewayRestAwsHttpAdapter(request, LambdaContextMock()).getOrThrow(),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .body("input body")
            ))
    }

    @Test
    fun `converts into http4k request with multi header`() {
        val request = mapOf(
            "path" to "/path",
            "body" to "",
            "headers" to mapOf("a" to "b", "c" to "f"),
            "multiValueHeaders" to mapOf("c" to listOf("d", "e", "f"), "g" to listOf("h")),
            "isBase64Encoded" to false,
            "httpMethod" to "GET"
        )

        assertThat(
            ApiGatewayRestAwsHttpAdapter(request, LambdaContextMock()).getOrThrow(),
            equalTo(Request(GET, "/path")
                .header("a", "b")
                .header("c", "d")
                .header("c", "e")
                .header("c", "f")
                .header("g", "h")
                .body("")
            ))
    }

    @Test
    fun `converts into http4k request with multi query`() {
        val request = mapOf(
            "path" to "/path",
            "body" to "",
            "queryStringParameters" to mapOf("a" to "b", "c" to "f"),
            "multiValueQueryStringParameters" to mapOf("c" to listOf("d", "e", "f"), "g" to listOf("h")),
            "isBase64Encoded" to false,
            "httpMethod" to "GET"
        )

        assertThat(
            ApiGatewayRestAwsHttpAdapter(request, LambdaContextMock()).getOrThrow(),
            equalTo(Request(GET, "/path")
                .query("c", "d")
                .query("c", "e")
                .query("c", "f")
                .query("g", "h")
                .query("a", "b")
                .body("")
            ))
    }

    @Test
    fun `converts into http4k request when body is not base 64 encoded`() {
        val request = mapOf(
            "path" to "/path",
            "queryStringParameters" to mapOf("query" to "value"),
            "body" to "input body",
            "headers" to mapOf("c" to "d"),
            "isBase64Encoded" to false,
            "httpMethod" to "GET"
        )

        assertThat(
            ApiGatewayRestAwsHttpAdapter(request, LambdaContextMock()).getOrThrow(),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .body("input body")
            ))
    }

    @Test
    fun `handles binary data`() {
        val imageBytes = this::class.java.getResourceAsStream("/test.png").readBytes()

        val request = mapOf(
            "path" to "/path",
            "body" to imageBytes.base64Encode(),
            "isBase64Encoded" to true,
            "httpMethod" to "POST"
        )

        assertThat(
            ApiGatewayRestAwsHttpAdapter(request, LambdaContextMock()).getOrThrow(),
            equalTo(Request(POST, "/path")
                .body(Body(ByteBuffer.wrap(imageBytes)))
            ))
    }

    @Test
    fun `converts from http4k response`() {
        assertThat(
            ApiGatewayRestAwsHttpAdapter(Response(Status.I_M_A_TEAPOT)
                .header("c", "d")
                .header("c", "e")
                .body("output body")
            ),
            equalTo(mapOf(
                "statusCode" to 418,
                "body" to "output body",
                "headers" to mapOf("c" to "e"),
                "isBase64Encoded" to false,
            ))
        )
    }
}
