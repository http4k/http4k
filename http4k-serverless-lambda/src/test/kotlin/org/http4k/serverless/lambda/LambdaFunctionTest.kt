package org.http4k.serverless.lambda

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.serverless.BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS
import org.http4k.serverless.TestApp
import org.junit.jupiter.api.Test

class LambdaFunctionTest {

    @Test
    fun `loads app from the environment and adapts API Gateway request and response`() {
        val request = ApiGatewayProxyRequest()
        request.httpMethod = "GET"
        request.body = "input body"
        request.headers = mapOf("c" to "d")
        request.path = "/path"
        request.queryStringParameters = mapOf("query" to "value")

        val env = mapOf(
            HTTP4K_BOOTSTRAP_CLASS to TestApp::class.java.name,
            "a" to "b")
        val response = LambdaFunction(env).handle(request)

        assertThat(response.statusCode, equalTo(201))
        assertThat(response.headers, equalTo(env))
        assertThat(response.body, equalTo(Request(GET, "/path")
            .header("c", "d")
            .body("input body")
            .query("query", "value").toString()))
    }

}