package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
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
import java.util.*

class ApiGatewayV2AwsHttpAdapterTest {

    @Test
    fun `converts into http4k request`() {
        val request = AwsGatewayProxyRequestV2(requestContext = RequestContext(Http("GET"))).apply {
            rawPath = "/path"
            queryStringParameters = mapOf("query" to "value")
            body = "input body"
            headers = mapOf("c" to "d")
        }

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

        val request = AwsGatewayProxyRequestV2(requestContext = RequestContext(Http("POST"))).apply{
            rawPath = "/"
            body = String(Base64.getEncoder().encode(imageBytes))
            isBase64Encoded = true
        }

        assertThat(
            ApiGatewayV2AwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(POST, "/")
                .body(Body(ByteBuffer.wrap(imageBytes)))
            ))
    }

    @Test
    fun `converts into http4k request when body is base 64 encoded`() {
        val request =  AwsGatewayProxyRequestV2(requestContext = RequestContext(Http("GET"))).apply {
            rawPath = "/path"
            queryStringParameters = mapOf("query" to "value")
            body = "input body".base64Encode()
            headers = mapOf("c" to "d")
            isBase64Encoded = true
        }

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
        val response = APIGatewayV2HTTPResponse.builder()
            .withStatusCode(418)
            .withBody("output body")
            .withCookies(emptyList())
            .withMultiValueHeaders(mapOf("c" to listOf("d")))
            .withHeaders(mapOf("c" to "d"))
            .build()

        assertThat(
            ApiGatewayV2AwsHttpAdapter(Response(Status.I_M_A_TEAPOT)
                .header("c", "d")
                .body("output body")
            ),
            equalTo(response)
        )
    }
}
