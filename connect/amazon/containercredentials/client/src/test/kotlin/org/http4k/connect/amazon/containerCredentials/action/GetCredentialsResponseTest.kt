package org.http4k.connect.amazon.containerCredentials.action

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.containercredentials.action.GetCredentialsResponse
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class GetCredentialsResponseTest {

    @Test
    fun `converts credentials response to credentials`() {
        val token = SessionToken.of("SessionToken")
        val accessKeyId = AccessKeyId.of("access-AccessKey")
        val secretAccessKey = SecretAccessKey.of("SecretAccessKey")
        val expiration = Expiration.of(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")))
        val validArn = "arn:aws:sts:us-east-1:000000000001:role:myrole"
        val notSuppliedArn = "NOT_SUPPLIED"

        assertThat(
            GetCredentialsResponse(token, accessKeyId, secretAccessKey, expiration, validArn).asCredentials(),
            equalTo(Credentials(token, accessKeyId, secretAccessKey, expiration, ARN.of(validArn)))
        )

        assertThat(
            GetCredentialsResponse(token, accessKeyId, secretAccessKey, expiration, notSuppliedArn).asCredentials(),
            equalTo(Credentials(token, accessKeyId, secretAccessKey, expiration, null))
        )

        assertThat(
            GetCredentialsResponse(token, accessKeyId, secretAccessKey, expiration, null).asCredentials(),
            equalTo(Credentials(token, accessKeyId, secretAccessKey, expiration, null))
        )
    }

}
