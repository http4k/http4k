package org.http4k.connect.amazon.lambda

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.lambda.action.invokeFunction
import org.http4k.connect.amazon.lambda.model.FunctionName
import org.http4k.core.MockHttp
import org.http4k.core.Uri
import org.http4k.hamkrest.hasUri
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class HttpLambdaTest {

    companion object {
        @JvmStatic
        fun requestUriSource() = listOf(
            Arguments.of(
                null,
                Uri.of("https://lambda.us-west-2.amazonaws.com/2015-03-31/functions/dummy/invocations")
            ),
            Arguments.of(
                Uri.of("http://localhost:8080/"),
                Uri.of("http://localhost:8080/2015-03-31/functions/dummy/invocations")
            )
        )
    }

    @ParameterizedTest
    @MethodSource("requestUriSource")
    fun `use correct request uri`(endpoint: Uri?, expectedRequestUri: Uri) {
        // given
        val mockHttp = MockHttp()
        val lambda = Lambda.Http(
            Region.US_WEST_2,
            CredentialsProvider.FakeAwsEnvironment(),
            mockHttp,
            overrideEndpoint = endpoint
        )

        // when
        runCatching {
            lambda.invokeFunction<String>(FunctionName.of("dummy"), "value")
        }

        // then
        assertThat(mockHttp.request, present(hasUri(expectedRequestUri)))
    }
}
