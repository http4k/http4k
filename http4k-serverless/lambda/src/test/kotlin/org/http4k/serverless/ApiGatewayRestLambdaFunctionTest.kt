package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.asFormatString
import org.junit.jupiter.api.Test

class ApiGatewayRestLambdaFunctionTest {

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

        val lambda = object : ApiGatewayRestLambdaFunction(AppLoaderWithContexts { env, contexts ->
            {
                assertThat(contexts[it].get<Context>(LAMBDA_CONTEXT_KEY), equalTo(lambdaContext))
                assertThat(contexts[it].get<Request>(LAMBDA_REQUEST_KEY), equalTo(request))
                assertThat(env, equalTo(System.getenv()))
                Response(OK).header("a", "b").body("hello there")
            }
        }) {}

        assertThat(
            asFormatString(lambda.handleRequest(request, lambdaContext)),
            equalTo(
                asFormatString(
                    mapOf(
                        "statusCode" to 200,
                        "headers" to mapOf("a" to "b"),
                        "body" to "hello there",
                        "isBase64Encoded" to false
                    )
                )
            )
        )
    }
}
