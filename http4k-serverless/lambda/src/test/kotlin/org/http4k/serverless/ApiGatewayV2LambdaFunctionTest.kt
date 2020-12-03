package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class ApiGatewayV2LambdaFunctionTest {

    @Test
    fun `adapts API Gateway request and response and receives context`() {
        val lambdaContext = LambdaContextMock()

        val request = mapOf(
            "rawPath" to "/path",
            "queryStringParameters" to mapOf("query" to "value"),
            "body" to "input body",
            "headers" to mapOf("c" to "d"),
            "requestContext" to mapOf("http" to mapOf("method" to "GET"))
        )

        val lambda = object : ApiGatewayV2LambdaFunction(AppLoaderWithContexts { env, contexts ->
            {
                assertThat(contexts[it][LAMBDA_CONTEXT_KEY], equalTo(lambdaContext))
                assertThat(contexts[it][LAMBDA_REQUEST_KEY], equalTo(request))
                assertThat(env, equalTo(System.getenv()))
                assertThat(it.removeHeader("x-http4k-context"), equalTo(Request(GET, "/path")
                    .query("query", "value")
                    .header("c", "d")
                    .body("input body")
                ))
                Response(Status.OK).header("a", "b").body("hello there")
            }
        }) {}

        val out = lambda.handle(request, lambdaContext)

        assertThat(out,
            equalTo(
                mapOf(
                    "statusCode" to 200,
                    "cookies" to emptyList<String>(),
                    "body" to "hello there",
                    "headers" to mapOf("a" to "b"),
                    "multiValueHeaders" to mapOf("a" to listOf("b")),
                )
            )
        )
    }
}
