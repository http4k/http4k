package org.http4k.connect.amazon.s3.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class BucketNameTest {
    @Test
    fun `can construct uri`() {
        assertThat(
            BucketName.of("foobar").toUri(Region.of("eu-west-1")),
            equalTo(Uri.of("https://foobar.s3.eu-west-1.amazonaws.com"))
        )
    }

    @Test
    fun `can construct uri for path style`() {
        assertThat(
            BucketName.of("foo.bar").toUri(Region.of("eu-west-1")),
            equalTo(Uri.of("https://s3.eu-west-1.amazonaws.com/foo.bar"))
        )
    }

    @Test
    fun `can construct uri with forced path style`() {
        assertThat(
            BucketName.of("foobar").toUri(Region.of("eu-west-1"), true),
            equalTo(Uri.of("https://s3.eu-west-1.amazonaws.com/foobar"))
        )
    }
}
