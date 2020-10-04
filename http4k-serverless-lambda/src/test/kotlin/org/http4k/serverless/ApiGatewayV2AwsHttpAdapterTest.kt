package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class ApiGatewayV2AwsHttpAdapterTest {

    @Test
    fun `converts into http4k request`() {
        val request = APIGatewayV2HTTPEvent.builder()
            .withRawPath("/path")
            .withQueryStringParameters(mapOf("query" to "value"))
            .withBody("input body")
            .withHeaders(mapOf("c" to "d"))
            .withRequestContext(APIGatewayV2HTTPEvent.RequestContext.builder()
                .withHttp(
                    APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod("GET").build()
                ).build())
            .build()

        assertThat(
            ApiGatewayV2AwsHttpAdapter(request),
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
            .withMultiValueHeaders(mapOf("c" to listOf("d")))
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
