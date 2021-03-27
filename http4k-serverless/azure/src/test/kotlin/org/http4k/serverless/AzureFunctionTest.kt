package org.http4k.serverless

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.sameInstance
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.util.proxy
import org.junit.jupiter.api.Test
import java.util.Optional

class AzureFunctionTest {

    @Test
    fun `adapts Azure request and response and receives context`() {
        val context: ExecutionContext = proxy()
        val request: HttpRequestMessage<Optional<String>> = FakeAzureRequest(
            Request(GET, "/path")
                .header("c", "d")
                .query("query", "value")
                .body("input body")
        )

        val response = Response(Status(200, "")).header("a", "b").body("hello there")

        val function = object : AzureFunction(AppLoaderWithContexts { env, contexts ->
            {
                assertThat(contexts[it][AZURE_CONTEXT_KEY], sameInstance(context))
                assertThat(contexts[it][AZURE_REQUEST_KEY], equalTo(request))
                assertThat(env, equalTo(System.getenv()))
                assertThat(it.removeHeader("x-http4k-context-azure"), equalTo(Request(GET, "/path")
                    .header("c", "d")
                    .body("input body")
                    .query("query", "value")))
                response
            }
        }) {
            override fun handleRequest(req: HttpRequestMessage<Optional<String>>,
                                       ctx: ExecutionContext): HttpResponseMessage = handle(req, ctx)
        }

        assertThat(function.handleRequest(request, context),
            equalTo(Http4kResponse(response))
        )
    }
}
