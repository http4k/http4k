package org.http4k.connect.amazon.sqs

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.MockHttp
import org.http4k.core.Uri
import org.http4k.hamkrest.hasUri
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class HttpSQSTest {

    companion object {
        @JvmStatic
        fun requestUriSource() = listOf(
            Arguments.of(null, Uri.of("https://sqs.me-south-1.amazonaws.com")),
            Arguments.of(Uri.of("http://localhost:8080/"), Uri.of("http://localhost:8080"))
        )
    }

    @ParameterizedTest
    @MethodSource("requestUriSource")
    fun `use correct request uri`(endpoint: Uri?, expectedRequestUri: Uri) {
        // given
        val mockHttp = MockHttp()
        val sqs = SQS.Http(
            Region.ME_SOUTH_1,
            CredentialsProvider.FakeAwsEnvironment(),
            mockHttp,
            overrideEndpoint = endpoint
        )

        // when
        runCatching {
            sqs.listQueues()
        }

        // then
        assertThat(mockHttp.request, present(hasUri(expectedRequestUri)))
    }
}
