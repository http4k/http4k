package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
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
import java.util.Base64

class ApiGatewayV1AwsHttpAdapterTest {

    @Test
    fun `converts into http4k request`() {
        val request = APIGatewayProxyRequestEvent().apply {
            httpMethod = "GET"
            body = "input body"
            headers = mapOf("c" to "d")
            path = "/path"
            queryStringParameters = mapOf("query" to "value")
        }

        assertThat(
            ApiGatewayV1AwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .body("input body")
            ))
    }

    @Test
    fun `converts into http4k request when body is base 64 encoded`() {
        val request = APIGatewayProxyRequestEvent().apply {
            httpMethod = "GET"
            body = "input body".base64Encode()
            headers = mapOf("c" to "d")
            path = "/path"
            queryStringParameters = mapOf("query" to "value")
            isBase64Encoded = true
        }

        assertThat(
            ApiGatewayV1AwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .body("input body")
            ))
    }

    @Test
    fun `handles binary data`() {
        val imageBytes = this::class.java.getResourceAsStream("/test.png").readBytes()

        val request = APIGatewayProxyRequestEvent().apply {
            httpMethod = "POST"
            body = String(Base64.getEncoder().encode(imageBytes))
            path = "/path"
            isBase64Encoded = true
        }

        assertThat(
            ApiGatewayV1AwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(POST, "/path")
                .body(Body(ByteBuffer.wrap(imageBytes)))
            ))
    }

    @Test
    fun `converts from http4k response`() {
        assertThat(
            ApiGatewayV1AwsHttpAdapter(Response(Status.I_M_A_TEAPOT)
                .header("c", "d")
                .body("output body")
            ),
            equalTo(APIGatewayProxyResponseEvent().apply {
                statusCode = 418
                body = "output body".base64Encode()
                headers = mapOf("c" to "d")
                isBase64Encoded = true
            })
        )
    }
}
