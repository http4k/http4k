package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class ApplicationLoadBalancerLambdaFunctionTest {

    @Test
    fun `adapts API Gateway request and response and receives context`() {
        val lambdaContext = LambdaContextMock()

        val request = ApplicationLoadBalancerRequestEvent().apply {
            httpMethod = "GET"
            body = "input body"
            headers = mapOf("c" to "d")
            path = "/path"
            queryStringParameters = mapOf("query" to "value")
            requestContext = ApplicationLoadBalancerRequestEvent.RequestContext()
        }

        val lambda = object : ApplicationLoadBalancerLambdaFunction(AppLoaderWithContexts { env, contexts ->
            HttpHandler {
                assertThat(contexts[it][LAMBDA_CONTEXT_KEY], equalTo(lambdaContext))
                assertThat(contexts[it][LAMBDA_REQUEST_KEY], equalTo(request))
                assertThat(env, equalTo(System.getenv()))
                assertThat(it.removeHeader("x-http4k-context"), equalTo(Request(GET, "/path")
                    .header("c", "d")
                    .body("input body")
                    .query("query", "value")))
                Response(Status.OK).header("a", "b").body("hello there")
            }
        }) {}

        assertThat(lambda.handle(request, lambdaContext),
            equalTo(
                ApplicationLoadBalancerResponseEvent().apply {
                    statusCode = 200
                    body = "hello there"
                    headers = mapOf("a" to "b")
                })
        )
    }
}
