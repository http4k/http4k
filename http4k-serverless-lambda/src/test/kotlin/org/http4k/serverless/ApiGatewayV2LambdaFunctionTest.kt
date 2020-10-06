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

class ApiGatewayV2LambdaFunctionTest {

    @Test
    fun `adapts API Gateway request and response and receives context`() {
        val lambdaContext = LambdaContextMock()

        val request = APIGatewayV2HTTPEvent.builder()
            .withRawPath("/path")
            .withQueryStringParameters(mapOf("query" to "value"))
            .withBody("input body")
            .withHeaders(mapOf("c" to "d"))
            .withRequestContext(APIGatewayV2HTTPEvent.RequestContext.builder()
                .withHttp(
                    APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod("GET").build()
                ).build()
            )
            .build()

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

        assertThat(lambda.handle(request, lambdaContext),
            equalTo(
                APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withBody("hello there")
                    .withHeaders(mapOf("a" to "b"))
                    .withMultiValueHeaders(mapOf("a" to listOf("b")))
                    .build()
            )
        )
    }
}
