package org.http4k.connect.amazon.sts

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.http4k.connect.RemoteFailure
import org.http4k.connect.TestClock
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.core.model.WebIdentityToken
import org.http4k.connect.failureValue
import org.http4k.connect.successValue
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Random

class FakeSTSTest : STSContract, FakeAwsContract {

    override val clock = TestClock()
    override val http = FakeSTS(clock = clock, random = Random(42))

    @Test
    fun `assume role with web identity and get caller identity`() {
        val assumedRole = sts.assumeRoleWithWebIdentity(
            ARN.of("arn:aws:iam::169766454405:role/ROLETEST"),
            RoleSessionName.of("web-role-session-name"),
            DurationSeconds = Duration.ofHours(2),
            WebIdentityToken = WebIdentityToken.of("web-token")
        ).successValue()

        assertThat(assumedRole.AssumedRoleId.value, equalTo("web-role-session-name"))
        assertThat(
            assumedRole.Credentials.Expiration.value,
            equalTo(ZonedDateTime.now(clock).plus(Duration.ofHours(2)))
        )

        val identity = STS.Http(aws.region, assumedRole.Credentials.provider(), http, clock)
            .getCallerIdentity()
            .successValue()

        assertThat(identity.Account.value, equalTo("169766454405"))
        assertThat(identity.UserId, equalTo("ARO123EXAMPLE123:web-role-session-name"))
        assertThat(identity.Arn.value, equalTo("arn:aws:sts::169766454405:assumed-role/ROLETEST/web-role-session-name"))
    }

    @Test
    fun `assume role and get caller identity`() {
        val assumedRole = sts.assumeRole(
            RoleArn = ARN.of("arn:aws:sts::123456789012:role/my-role-name"),
            RoleSessionName = RoleSessionName.of("my-role-session-name"),
            DurationSeconds = Duration.ofHours(3)
        ).successValue()

        assertThat(assumedRole.AssumedRoleId.value, equalTo("my-role-session-name"))
        assertThat(
            assumedRole.Credentials.Expiration.value,
            equalTo(ZonedDateTime.now(clock).plus(Duration.ofHours(3)))
        )

        val identity = STS.Http(aws.region, assumedRole.Credentials.provider(), http, clock)
            .getCallerIdentity()
            .successValue()

        assertThat(identity.Account.value, equalTo("123456789012"))
        assertThat(identity.UserId, equalTo("ARO123EXAMPLE123:my-role-session-name"))
        assertThat(identity.Arn.value, equalTo("arn:aws:sts::123456789012:assumed-role/my-role-name/my-role-session-name"))
    }

    @Test
    fun `get caller identity for expired role`() {
        val assumedRole = sts.assumeRole(
            RoleArn = ARN.of("arn:aws:sts::123456789012:role/my-role-name"),
            RoleSessionName = RoleSessionName.of("my-role-session-name"),
            DurationSeconds = Duration.ofMinutes(10)
        ).successValue()

        val assumedSts = STS.Http(aws.region, assumedRole.Credentials.provider(), http, clock)
        assumedSts.getCallerIdentity().successValue()

        clock.tickBy(Duration.ofMinutes(20))
        assertThat(
            assumedSts.getCallerIdentity().failureValue(),
            equalTo(RemoteFailure(Method.POST, Uri.of(""), Status.UNAUTHORIZED, ""))
        )
    }
}

private fun Credentials.provider() = CredentialsProvider {
    AwsCredentials(
        accessKey = AccessKeyId.value,
        secretKey = SecretAccessKey.value,
        sessionToken = Token.value
    )
}
