package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class InvocationLambdaAwsHttpAdapterTest {

    @Test
    fun `converts into http4k request`() {
        val request = "helloworld"
        assertThat(
            InvocationLambdaAwsHttpAdapter(request, LambdaContextMock()),
            equalTo(Request(POST, "/2015-03-31/functions/LambdaContextMock/invocations")
                .header("X-Amz-Invocation-Type", "RequestResponse")
                .header("X-Amz-Log-Type", "Tail")
                .body(request)
            ))
    }

    @Test
    fun `converts from http4k response - throws everything away`() {
        assertThat(
            InvocationLambdaAwsHttpAdapter(Response(Status.I_M_A_TEAPOT)
                .header("c", "d")
                .body("output body")
            ),
            equalTo("output body")
        )
    }
}
