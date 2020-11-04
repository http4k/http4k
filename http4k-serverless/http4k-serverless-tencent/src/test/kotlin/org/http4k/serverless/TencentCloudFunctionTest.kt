package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.sameInstance
import com.qcloud.scf.runtime.Context
import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.util.proxy
import org.junit.jupiter.api.Test

class TencentCloudFunctionTest {

    @Test
    fun `adapts API Gateway request and response and receives context`() {

        val context: Context = proxy()

        val request = APIGatewayProxyRequestEvent().apply {
            httpMethod = "GET"
            body = "input body"
            headers = mapOf("c" to "d")
            path = "/path"
            queryStringParameters = mapOf("query" to "value")
        }

        val tencent = object : TencentCloudFunction(AppLoaderWithContexts { env, contexts ->
            {
                assertThat(contexts[it][TENCENT_CONTEXT_KEY], sameInstance(context))
                assertThat(contexts[it][TENCENT_REQUEST_KEY], equalTo(request))
                assertThat(env, equalTo(System.getenv()))
                assertThat(it.removeHeader("x-http4k-context"), equalTo(Request(GET, "/path")
                    .header("c", "d")
                    .body("input body")
                    .query("query", "value")))
                Response(OK).header("a", "b").body("hello there")
            }
        }) {}

        assertThat(tencent.handleRequest(request, context),
            equalTo(
                APIGatewayProxyResponseEvent().apply {
                    statusCode = 200
                    body = "hello there"
                    headers = mapOf("a" to "b")
                })
        )
    }
}
