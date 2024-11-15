package org.http4k.connect.amazon.kms

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

class HttpKMSTest {

    companion object {
        @JvmStatic
        fun requestUriSource() = listOf(
            Arguments.of(null, Uri.of("https://kms.us-west-1.amazonaws.com/")),
            Arguments.of(Uri.of("http://localhost:8080/"), Uri.of("http://localhost:8080/"))
        )
    }

    @ParameterizedTest
    @MethodSource("requestUriSource")
    fun `use correct request uri`(endpoint: Uri?, expectedRequestUri: Uri) {
        // given
        val mockHttp = MockHttp()
        val kms = KMS.Http(
            Region.US_WEST_1,
            CredentialsProvider.FakeAwsEnvironment(),
            mockHttp,
            overrideEndpoint = endpoint
        )

        // when
        runCatching {
            kms.createKey()
        }

        // then
        assertThat(mockHttp.request, present(hasUri(expectedRequestUri)))
    }
}
