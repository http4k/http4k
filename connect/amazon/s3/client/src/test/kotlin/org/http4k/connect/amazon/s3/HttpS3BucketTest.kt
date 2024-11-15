package org.http4k.connect.amazon.s3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.fakeAwsEnvironment
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.core.MockHttp
import org.http4k.core.Uri
import org.http4k.hamkrest.hasUri
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class HttpS3BucketTest {

    companion object {
        @JvmStatic
        fun requestUriSource(): List<Arguments> {
            return listOf(
                Arguments.of(
                    BucketName.of("host-style-bucket"),
                    null,
                    false,
                    Uri.of("https://host-style-bucket.s3.us-east-1.amazonaws.com/key")
                ),
                Arguments.of(
                    BucketName.of("path.style.bucket"),
                    null,
                    false,
                    Uri.of("https://s3.us-east-1.amazonaws.com/path.style.bucket/key")
                ),
                Arguments.of(
                    BucketName.of("host-style-bucket"),
                    null,
                    true,
                    Uri.of("https://s3.us-east-1.amazonaws.com/host-style-bucket/key")
                ),
                Arguments.of(
                    BucketName.of("host-style-bucket"),
                    Uri.of("http://localhost:9000"),
                    true,
                    Uri.of("http://localhost:9000/host-style-bucket/key")
                ),
                Arguments.of(
                    BucketName.of("path.style.bucket"),
                    Uri.of("http://localhost:9000"),
                    false,
                    Uri.of("http://localhost:9000/path.style.bucket/key")
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("requestUriSource")
    fun `builds correct request uri`(
        bucketName: BucketName,
        endpoint: Uri?,
        forcePathStyle: Boolean,
        expectedUri: Uri
    ) {
        // given
        val mockHttp = MockHttp()
        val bucket = S3Bucket.Http(
            bucketName = bucketName,
            bucketRegion = Region.US_EAST_1,
            credentialsProvider = { fakeAwsEnvironment.credentials },
            http = mockHttp,
            overrideEndpoint = endpoint,
            forcePathStyle = forcePathStyle
        )

        // when
        bucket[BucketKey.of("key")]

        // then
        assertThat(mockHttp.request, present(hasUri(expectedUri)))
    }
}
