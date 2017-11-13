package org.http4k.serverless.lambda

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.Test

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
            BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS to TestApp::class.java.name,
            "a" to "b")
        val response = LambdaFunction(env).handle(request)

        response.statusCode shouldMatch equalTo(201)
        response.headers shouldMatch equalTo(env)
        response.body shouldMatch equalTo(Request(GET, "/path")
            .header("c", "d")
            .body("input body")
            .query("query", "value").toString())
    }

}