package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.*

class ApiGatewayV2AwsHttpAdapterTest {

    @Test
    fun `converts into http4k request`() {
        val request = mapOf(
            "rawPath" to "/path",
            "queryStringParameters" to mapOf("query" to "value"),
            "body" to "input body",
            "headers" to mapOf("c" to "d"),
            "requestContext" to mapOf("http" to mapOf("method" to "GET"))
        )

        assertThat(
            ApiGatewayV2AwsHttpAdapter(request, LambdaContextMock()),
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
            "rawPath" to "/",
            "body" to String(Base64.getEncoder().encode(imageBytes)),
            "isBase64Encoded" to true,
            "requestContext" to mapOf("http" to mapOf("method" to "POST"))
        )

        assertThat(
            ApiGatewayV2AwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(Method.POST, "/")
                .body(Body(ByteBuffer.wrap(imageBytes)))
            ))
    }

    @Test
    fun `converts into http4k request when body is base 64 encoded`() {
        val request = mapOf(
            "rawPath" to "/path",
            "queryStringParameters" to mapOf("query" to "value"),
            "body" to "input body".base64Encode(),
            "headers" to mapOf("c" to "d"),
            "isBase64Encoded" to true,
            "requestContext" to mapOf("http" to mapOf("method" to "GET"))
        )

        assertThat(
            ApiGatewayV2AwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .body("input body")
            ))
    }

    @Test
    fun `converts from http4k response`() {
        val response = mapOf(
            "statusCode" to 418,
            "body" to "output body",
            "cookies" to emptyList<String>(),
            "multiValueHeaders" to mapOf("c" to listOf("d")),
            "headers" to mapOf("c" to "d"),
        )

        assertThat(
            ApiGatewayV2AwsHttpAdapter(Response(Status.I_M_A_TEAPOT)
                .header("c", "d")
                .body("output body")
            ),
            equalTo(response)
        )
    }
}
