@file:Suppress("DEPRECATION")

package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.serverless.BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS
import org.junit.jupiter.api.Test

@Suppress("DEPRECATION")
class ApiGatewayV2LambdaFunctionTest {
    class LambdaContextMock(private val functionName: String = "LambdaContextMock") : Context {
        override fun getFunctionName() = functionName
        override fun getAwsRequestId() = TODO("LambdaContextMock")
        override fun getLogStreamName() = TODO("LambdaContextMock")
        override fun getInvokedFunctionArn() = TODO("LambdaContextMock")
        override fun getLogGroupName() = TODO("LambdaContextMock")
        override fun getFunctionVersion() = TODO("LambdaContextMock")
        override fun getIdentity() = TODO("LambdaContextMock")
        override fun getClientContext() = TODO("LambdaContextMock")
        override fun getRemainingTimeInMillis() = TODO("LambdaContextMock")
        override fun getLogger() = TODO("LambdaContextMock")
        override fun getMemoryLimitInMB() = TODO("LambdaContextMock")
    }

    @Test
    fun `loads app from the environment and adapts API Gateway v2 request and response, as well as include the lambda context in the request context`() {
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

        val env = mapOf(
            HTTP4K_BOOTSTRAP_CLASS to TestAppWithContexts::class.java.name,
            "a" to "b")

        val l = object : LambdaFunction(AppLoaderWithContexts { _, contexts ->
            LamdbdaTestAppWithContext<APIGatewayV2HTTPEvent>(env, contexts) { it.requestContext.accountId }
        }) {}
        val response = l.handle(request, LambdaContextMock(functionName = "TestFunction1"))

        assertThat(response.statusCode, equalTo(201))
        assertThat(response.headers, equalTo(env))
        assertThat(response.body, equalTo(Request(GET, "/path")
            .header("c", "d")
            .header("LAMBDA_CONTEXT_FUNCTION_NAME", "TestFunction1")
            .header("LAMBDA_REQUEST_VALUE", "123456789012")
            .body("input body")
            .query("query", "value").toString()))
    }
}
