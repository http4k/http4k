package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.base64Encode
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasMethod
import org.http4k.hamkrest.hasUri
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.Base64

class ApiGatewayV2AwsHttpAdapterTest {

    @Test
    fun `converts into http4k request`() {
        val inputCookie = Cookie("name", "value")
        val inputCookie2 = Cookie("name1", "value1")

        val request = APIGatewayV2HTTPEvent.builder()
            .withRawPath("/path")
            .withQueryStringParameters(mapOf("query" to "value"))
            .withBody("input body")
            .withHeaders(mapOf("c" to "d"))
            .withCookies(listOf(inputCookie.keyValueCookieString(), inputCookie2.keyValueCookieString()))
            .withRequestContext(APIGatewayV2HTTPEvent.RequestContext.builder()
                .withHttp(
                    APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod("GET").build()
                ).build())
            .build()

        assertThat(
            ApiGatewayV2AwsHttpAdapter(request, LambdaContextMock()),
            hasMethod(GET)
                .and(hasUri("/path?query=value"))
                .and(hasHeader("c", "d"))
                .and(has(Request::cookies, equalTo(listOf(inputCookie, inputCookie2))))
                .and(hasBody("input body"))
        )
    }

    @Test
    fun `handles binary data`() {
        val imageBytes = this::class.java.getResourceAsStream("/test.png").readBytes()

        val request = APIGatewayV2HTTPEvent.builder()
            .withRawPath("/")
            .withBody(String(Base64.getEncoder().encode(imageBytes)))
            .withIsBase64Encoded(true)
            .withRequestContext(APIGatewayV2HTTPEvent.RequestContext.builder()
                .withHttp(
                    APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod("POST").build()
                ).build())
            .build()

        assertThat(
            ApiGatewayV2AwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(POST, "/")
                .body(Body(ByteBuffer.wrap(imageBytes)))
            ))
    }

    @Test
    fun `converts into http4k request when body is base 64 encoded`() {
        val inputCookie = Cookie("name", "value")
        val inputCookie2 = Cookie("name1", "value1")

        val request = APIGatewayV2HTTPEvent.builder()
            .withRawPath("/path")
            .withQueryStringParameters(mapOf("query" to "value"))
            .withBody("input body".base64Encode())
            .withHeaders(mapOf("c" to "d"))
            .withCookies(listOf(inputCookie.fullCookieString() + "; " + inputCookie2.fullCookieString()))
            .withIsBase64Encoded(true)
            .withRequestContext(APIGatewayV2HTTPEvent.RequestContext.builder()
                .withHttp(
                    APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod("GET").build()
                ).build())
            .build()

        assertThat(
            ApiGatewayV2AwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .header("Cookie", inputCookie.fullCookieString() + "; " + inputCookie2.fullCookieString())
                .body("input body")
            ))
    }

    @Test
    fun `converts from http4k response`() {
        val response = APIGatewayV2HTTPResponse.builder()
            .withStatusCode(418)
            .withBody("output body")
            .withMultiValueHeaders(mapOf("c" to listOf("d")))
            .withHeaders(mapOf("c" to "d"))
            .withCookies(emptyList())
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
