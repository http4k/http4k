package org.http4k.connect.amazon.sts

import dev.forkhandles.result4k.map
import org.http4k.aws.AwsCredentials
import org.http4k.connect.TestClock
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.core.model.WebIdentityToken
import org.http4k.connect.successValue
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Random
import java.util.UUID

class FakeSTSTest : STSContract, FakeAwsContract {

    override val clock = TestClock()
    override val http = FakeSTS(clock = clock, random = Random(42))

    @Test
    fun `assume role with web identity`() {
        val result = sts.assumeRoleWithWebIdentity(
            ARN.of("arn:aws:iam::169766454405:role/ROLETEST"),
            RoleSessionName.of(UUID.randomUUID().toString()),
            DurationSeconds = Duration.ofHours(1),
            WebIdentityToken = WebIdentityToken.of("token")
        )

        assertTrue(
            result.successValue()
                .Credentials.Expiration.value.isAfter(ZonedDateTime.now(clock))
        )
    }

    @Test
    fun `get caller identity for assumed role`() {
        val credentials = sts.assumeRole(
            RoleArn = ARN.of("arn:aws:sts::123456789012:role/my-role-name"),
            RoleSessionName = RoleSessionName.of("my-role-session-name")
        )
            .map { AwsCredentials(it.Credentials.AccessKeyId.value, it.Credentials.SecretAccessKey.value, it.Credentials.Token.value) }
            .successValue()

        val response = STS.Http(aws.region, { credentials }, http, clock)
            .getCallerIdentity().successValue()

        assertEquals("123456789012", response.Account.value)
        assertEquals("ARO123EXAMPLE123:my-role-session-name", response.UserId)
        assertEquals(
            "arn:aws:sts::123456789012:assumed-role/my-role-name/my-role-session-name",
            response.Arn.value
        )
    }
}
