package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class ApplicationLoadBalancerHttpAdapterTest {

    @Test
    fun `converts into http4k request`() {
        val request = ApplicationLoadBalancerRequestEvent().apply {
            httpMethod = "GET"
            body = "input body"
            headers = mapOf("c" to "d")
            path = "/path"
            queryStringParameters = mapOf("query" to "value")
        }

        assertThat(
            ApplicationLoadBalancerAwsHttpAdapter(request),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .body("input body")
            ))
    }

    @Test
    fun `converts into http4k request when body is base 64 encoded`() {
        val request = ApplicationLoadBalancerRequestEvent().apply {
            httpMethod = "GET"
            body = "input body".base64Encode()
            headers = mapOf("c" to "d")
            path = "/path"
            queryStringParameters = mapOf("query" to "value")
            isBase64Encoded = true
        }

        assertThat(
            ApplicationLoadBalancerAwsHttpAdapter(request),
            equalTo(Request(GET, "/path")
                .query("query", "value")
                .header("c", "d")
                .body("input body")
            ))
    }

    @Test
    fun `converts from http4k response`() {
        val response = ApplicationLoadBalancerResponseEvent().apply {
            statusCode = 418
            body = "output body"
            headers = mapOf("c" to "d")
        }

        assertThat(
            ApplicationLoadBalancerAwsHttpAdapter(Response(Status.I_M_A_TEAPOT)
                .header("c", "d")
                .body("output body")
            ),
            equalTo(response)
        )
    }
}
