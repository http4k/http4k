package org.http4k.connect.amazon.sts

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.http4k.connect.TestClock
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.amazon.sts.action.AssumeRole
import org.http4k.connect.amazon.sts.action.AssumedRole
import org.http4k.connect.amazon.sts.action.SimpleAssumedRole
import org.http4k.connect.amazon.sts.model.RoleId
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class STSCredentialsProviderTest {

    private val sts = mockk<STS>()
    private val now = Instant.now()
    private val requestProvider: () -> STSAction<out AssumedRole> = { AssumeRole(arn, RoleSessionName.of("Session")) }
    private val clock = TestClock(now)

    private val provider = CredentialsProvider.STS(sts, clock, Duration.ofSeconds(60), requestProvider)

    @Test
    fun `gets credentials first time only`() {
        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)
        every { sts.invoke(any<AssumeRole>()) } returns assumedRole(firstCreds)

        assertThat(provider(), equalTo(firstCreds.asHttp4k()))
        clock.tickBy(Duration.ofSeconds(1))
        assertThat(provider(), equalTo(firstCreds.asHttp4k()))

        verify(exactly = 1) { sts.invoke(any<AssumeRole>()) }
    }

    @Test
    fun `gets credentials when expired time only`() {
        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)
        every { sts.invoke(any<AssumeRole>()) } returns assumedRole(firstCreds)

        assertThat(provider(), equalTo(firstCreds.asHttp4k()))

        verify(exactly = 1) { sts.invoke(any<AssumeRole>()) }

        val secondCreds = credentialsExpiringAt(now.plusSeconds(61), 2)
        every { sts.invoke(any<AssumeRole>()) } returns assumedRole(secondCreds)

        clock.tickBy(Duration.ofSeconds(1))
        assertThat(provider(), equalTo(firstCreds.asHttp4k()))

        verify(exactly = 1) { sts.invoke(any<AssumeRole>()) }

        clock.tickBy(Duration.ofMillis(1))
        assertThat(provider(), equalTo(secondCreds.asHttp4k()))

        verify(exactly = 2) { sts.invoke(any<AssumeRole>()) }
    }

    private fun assumedRole(credentials: Credentials) = Success(
        SimpleAssumedRole(
            RoleId.of("hello"),
            credentials
        )
    )

    private fun credentialsExpiringAt(expiry: Instant, counter: Int) = Credentials(
        SessionToken.of("SessionToken"),
        AccessKeyId.of(counter.toString()),
        SecretAccessKey.of("SecretAccessKey"),
        Expiration.of(ZonedDateTime.ofInstant(expiry, ZoneId.of("UTC"))),
        ARN.of("arn:aws:sts:us-east-1:000000000001:role:myrole")
    )

    private val arn = ARN.of("arn:aws:foobar")
}
