package org.http4k.connect.amazon

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.http4k.config.Environment
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.core.with
import org.junit.jupiter.api.Test

class EnvironmentCredentialsChainTest {

    @Test
    fun `find credentials`() {
        val env = Environment.EMPTY
            .with(AWS_ACCESS_KEY_ID of AccessKeyId.of("key123"))
            .with(AWS_SECRET_ACCESS_KEY of SecretAccessKey.of("secret123"))

        assertThat(
            CredentialsChain.Environment(env).invoke(),
            equalTo(AwsCredentials("key123", "secret123"))
        )
    }

    @Test
    fun `missing credentials`() {
        assertThat(
            CredentialsChain.Environment(Environment.EMPTY).invoke(),
            absent()
        )
    }
}
