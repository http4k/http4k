package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class ApiGatewayV2LambdaFunctionTest {

    @Test
    fun `adapts API Gateway request and response and receives context`() {
        val lambdaContext = LambdaContextMock()

        val request =  AwsGatewayProxyRequestV2(requestContext = RequestContext(Http("GET"))).apply {
            rawPath = "/path"
            queryStringParameters = mapOf("query" to "value")
            body = "input body"
            headers = mapOf("c" to "d")
        }

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

        val out = ByteArrayOutputStream()
        lambda.handleRequest(Jackson.asFormatString(request).byteInputStream(), out, lambdaContext)

        assertThat(Jackson.asA(out.toString()),
            equalTo(
                APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withCookies(emptyList())
                    .withBody("hello there")
                    .withHeaders(mapOf("a" to "b"))
                    .withCookies(emptyList())
                    .withMultiValueHeaders(mapOf("a" to listOf("b")))
                    .build()
            )
        )
    }
}
