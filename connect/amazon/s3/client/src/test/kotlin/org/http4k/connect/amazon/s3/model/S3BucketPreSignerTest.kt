package org.http4k.connect.amazon.s3.model

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.aws.AwsCredentials
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.Uri
import org.http4k.hamkrest.hasHost
import org.http4k.hamkrest.hasUriPath
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration


class S3BucketPreSignerTest {

    @Test
    fun `using alternative urls`() {

        val signer = S3BucketPreSigner(
            bucketName = BucketName.of("lovely-bucket"),
            region = Region.of("us-west-000"),
            credentials = AwsCredentials("access-key", "secret-key"),
            clock = Clock.systemUTC(),
            overrideEndpoint = Uri.of("https://s3.us-west-000.backblazeb2.com")
        )

        val request = signer.get(key = BucketKey.of("happy-key"), duration = Duration.ofMinutes(1))

        assertThat(request.uri, hasHost("s3.us-west-000.backblazeb2.com"))
        assertThat(request.uri, hasUriPath("/lovely-bucket/happy-key"))
    }
}
