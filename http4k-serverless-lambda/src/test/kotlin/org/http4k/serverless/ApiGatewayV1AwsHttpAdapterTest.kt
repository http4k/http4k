package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

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
            ApiGatewayV1AwsHttpAdapter(request),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .body("input body")
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
                body = "output body"
                headers = mapOf("c" to "d")
            })
        )
    }
}
