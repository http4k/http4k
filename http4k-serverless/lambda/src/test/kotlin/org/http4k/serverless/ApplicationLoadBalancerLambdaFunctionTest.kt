package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class ApplicationLoadBalancerLambdaFunctionTest {

    @Test
    fun `adapts API Gateway request and response and receives context`() {
        val lambdaContext = LambdaContextMock()

        val request = mapOf(
            "path" to "/path",
            "queryStringParameters" to mapOf("query" to "value"),
            "body" to "input body",
            "headers" to mapOf("c" to "d"),
            "isBase64Encoded" to false,
            "httpMethod" to "GET"
        )

        val lambda = object : ApplicationLoadBalancerLambdaFunction(AppLoaderWithContexts { env, contexts ->
            {
                assertThat(contexts[it][LAMBDA_CONTEXT_KEY], equalTo(lambdaContext))
                assertThat(contexts[it][LAMBDA_REQUEST_KEY], equalTo(request))
                assertThat(env, equalTo(System.getenv()))
                assertThat(it.removeHeader("x-http4k-context-lambda"), equalTo(Request(GET, "/path")
                    .header("c", "d")
                    .body("input body")
                    .query("query", "value")))
                Response(Status.OK).header("a", "b").body("hello there")
            }
        }) {}

        assertThat(lambda.handleRequest(request, lambdaContext),
            equalTo(
                mapOf(
                    "statusCode" to 200,
                    "body" to "hello there".base64Encode(),
                    "headers" to mapOf("a" to "b"),
                    "isBase64Encoded" to true
                )
            )
        )
    }
}
