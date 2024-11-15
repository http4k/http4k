package org.http4k.connect.amazon.sts

import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.core.model.WebIdentityToken
import org.http4k.connect.successValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

class FakeSTSTest : STSContract, FakeAwsContract {

    override val http = FakeSTS()

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
}
