package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class AwsCredentialsTest {
    @Test
    fun `toString redacts secretKey and sessionToken`() {
        assertThat(
            AwsCredentials("AKIAEXAMPLE", "verySecretKey", "verySecretToken").toString(),
            equalTo("AwsCredentials(accessKey=AKIAEXAMPLE, secretKey=****, sessionToken=****)")
        )
    }

    @Test
    fun `toString shows null sessionToken as null`() {
        assertThat(
            AwsCredentials("AKIAEXAMPLE", "verySecretKey").toString(),
            equalTo("AwsCredentials(accessKey=AKIAEXAMPLE, secretKey=****, sessionToken=null)")
        )
    }
}
