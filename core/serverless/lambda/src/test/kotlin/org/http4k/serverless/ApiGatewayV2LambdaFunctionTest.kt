package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.asFormatString
import org.junit.jupiter.api.Test

class ApiGatewayV2LambdaFunctionTest {

    @Test
    fun `adapts API Gateway request and response and receives context`() {
        val lambdaContext = LambdaContextMock()

        val request = mapOf(
            "rawPath" to "/path",
            "rawQueryString" to "query=value",
            "body" to "input body",
            "headers" to mapOf("c" to "d"),
            "requestContext" to mapOf("http" to mapOf("method" to "GET"))
        )

        val lambda = object : ApiGatewayV2LambdaFunction(AppLoader { env ->
            {
                assertThat(LAMBDA_CONTEXT_KEY(it), equalTo(lambdaContext))
                assertThat(LAMBDA_REQUEST_KEY(it), equalTo(request))
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
                        "multiValueHeaders" to mapOf("a" to listOf("b")),
                        "cookies" to emptyList<String>(),
                        "body" to "hello there".base64Encode(),
                        "isBase64Encoded" to true
                    )
                )
            )
        )
    }
}
