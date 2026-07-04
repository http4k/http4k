package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class ApiGatewayFnLoaderTest {

    @Test
    fun `unparseable event does not leak the exception message into the response body`() {
        val lambda = object : ApiGatewayV1LambdaFunction(AppLoader { { Response(OK) } }) {}

        val response = lambda.handleRequest(mapOf("path" to "/path"), LambdaContextMock())

        assertThat(response["statusCode"].toString(), equalTo("400"))
        assertThat(response["body"], equalTo(""))
    }
}
