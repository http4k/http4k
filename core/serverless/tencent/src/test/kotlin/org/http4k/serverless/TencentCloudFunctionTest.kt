package org.http4k.serverless

import com.alibaba.fastjson.JSONObject
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.sameInstance
import com.qcloud.scf.runtime.Context
import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent
import dev.forkhandles.mock4k.mock
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class TencentCloudFunctionTest {

    @Test
    fun `adapts APIGW request and response and receives context`() = runBlocking {

        val context: Context = mock()

        val request = APIGatewayProxyRequestEvent().apply {
            httpMethod = "GET"
            body = "input body"
            headers = mapOf("c" to "d")
            path = "/path"
            queryStringParameters = mapOf("query" to "value")
        }

        val tencent = object : TencentCloudFunction(AppLoader { env ->
            {
                assertThat(TENCENT_CONTEXT_KEY(it), sameInstance(context))
                assertThat(TENCENT_REQUEST_KEY(it), equalTo(request))
                assertThat(env, equalTo(System.getenv()))
                assertThat(
                    it.removeHeader("x-http4k-context-tencent"), equalTo(
                        Request(GET, "/path")
                            .header("c", "d")
                            .body("input body")
                            .query("query", "value")
                    )
                )
                Response(OK).header("a", "b").body("hello there")
            }
        }) {}

        assertThat(
            tencent.handleRequest(request, context),
            equalTo(
                APIGatewayProxyResponseEvent().apply {
                    statusCode = 200
                    body = "hello there"
                    headers = JSONObject(mapOf("a" to "b"))
                })
        )
    }
}
