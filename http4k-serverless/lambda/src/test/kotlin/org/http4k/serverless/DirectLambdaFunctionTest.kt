package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class DirectLambdaFunctionTest {

    @Test
    fun `adapts Direct request and response and receives context`() {
        val lambdaContext = LambdaContextMock()

        val request = "input body"

        val lambda = object : DirectLambdaFunction(AppLoaderWithContexts { env, contexts ->
            {
                assertThat(contexts[it][LAMBDA_CONTEXT_KEY], equalTo(lambdaContext))
                assertThat(contexts[it][LAMBDA_REQUEST_KEY], equalTo(request))
                assertThat(env, equalTo(System.getenv()))
                assertThat(it.removeHeader("x-http4k-context"), equalTo(Request(POST, "").body(request)))
                Response(OK).body(request + request)
            }
        }) {}

        assertThat(lambda.handle(request, lambdaContext), equalTo(request + request))
    }
}
