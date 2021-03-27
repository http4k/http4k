package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class InvocationLambdaFunctionTest {

    @Test
    fun `adapts Direct request and response and receives context`() {
        val lambdaContext = LambdaContextMock("myFunction")

        val request = "input body"

        val lambda = object : InvocationLambdaFunction(AppLoaderWithContexts { env, contexts ->
            {
                assertThat(contexts[it][LAMBDA_CONTEXT_KEY], equalTo(lambdaContext))
                assertThat(env, equalTo(System.getenv()))
                assertThat(it.removeHeader("x-http4k-context-lambda"), equalTo(
                    Request(POST, "/2015-03-31/functions/myFunction/invocations")
                        .header("X-Amz-Invocation-Type", "RequestResponse")
                        .header("X-Amz-Log-Type", "Tail")
                        .body(request)))
                Response(OK).body(request + request)
            }
        }) {}

        assertThat(
            lambda.handle(request.byteInputStream(), lambdaContext).reader().readText(),
            equalTo(request + request))
    }
}
