@file:Suppress("DEPRECATION")

package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.serverless.BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS
import org.junit.jupiter.api.Test

class ApplicationLoadBalancerLambdaFunctionTest {

    @Test
    fun `loads app from the environment and adapts API Gateway request and response`() {
        val request = ApplicationLoadBalancerRequestEvent()
        request.httpMethod = "GET"
        request.body = "input body"
        request.headers = mapOf("c" to "d")
        request.path = "/path"
        request.queryStringParameters = mapOf("query" to "value")

        val env = mapOf(
            HTTP4K_BOOTSTRAP_CLASS to TestApp::class.java.name,
            "a" to "b")

        val l = object : LambdaFunction(AppLoaderWithContexts { _, contexts ->
            LamdbdaTestAppWithContext<ApplicationLoadBalancerRequestEvent>(env, contexts) { it.requestContext.elb.targetGroupArn }
        }) {}
        val response = l.handle(request, LambdaContextMock())

        assertThat(response.statusCode, equalTo(201))
        assertThat(response.headers, equalTo(env))
        assertThat(response.body, equalTo(Request(GET, "/path")
            .header("c", "d")
            .body("input body")
            .query("query", "value").toString()))
    }

    @Test
    fun `loads function from the environment and adapts API Gateway request and response`() {
        val env = mapOf("a" to "b")

        class MyFunction : LambdaFunction(TestApp(env))

        val request = ApplicationLoadBalancerRequestEvent()
        request.httpMethod = "GET"
        request.body = "input body"
        request.headers = mapOf("c" to "d")
        request.path = "/path"
        request.queryStringParameters = mapOf("query" to "value")

        val response = MyFunction().handle(request, LambdaContextMock())

        assertThat(response.statusCode, equalTo(201))
        assertThat(response.headers, equalTo(env))
        assertThat(response.body, equalTo(Request(GET, "/path")
            .header("c", "d")
            .body("input body")
            .query("query", "value").toString()))
    }

    @Test
    fun `loads app from the environment and adapts API Gateway request and response, as well as include the lambda context in the request context`() {
        val request = ApplicationLoadBalancerRequestEvent()
        request.httpMethod = "GET"
        request.body = "input body"
        request.headers = mapOf("c" to "d")
        request.path = "/path"
        request.queryStringParameters = mapOf("query" to "value")
        request.requestContext = ApplicationLoadBalancerRequestEvent.RequestContext()

        val env = mapOf(
            HTTP4K_BOOTSTRAP_CLASS to TestAppWithContexts::class.java.name,
            "a" to "b")
        val l = object : LambdaFunction(AppLoaderWithContexts { _, contexts -> LamdbdaTestAppWithContext<ApplicationLoadBalancerRequestEvent>(env, contexts) { it.requestContext.elb.targetGroupArn } }) {}
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
