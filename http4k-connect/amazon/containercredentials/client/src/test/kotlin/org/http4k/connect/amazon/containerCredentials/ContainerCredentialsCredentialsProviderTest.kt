package org.http4k.connect.amazon.containerCredentials

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.http4k.config.Environment
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.containercredentials.ContainerCredentials
import org.http4k.connect.amazon.containercredentials.action.GetCredentials
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ContainerCredentialsCredentialsProviderTest {

    private val containerCredentials = mockk<ContainerCredentials>()
    private val now = Instant.now()
    private val clock = TestClock(now)

    private val relativePathUri = Uri.of("/hello")

    private val provider = CredentialsProvider.ContainerCredentials(
        containerCredentials,
        relativePathUri,
        clock,
        Duration.ofSeconds(60)
    )

    @Test
    fun `gets credentials first time only`() {
        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)
        every { containerCredentials(GetCredentials(relativePathUri)) } returns Success(firstCreds)

        assertThat(provider(), equalTo(firstCreds.asHttp4k()))
        clock.tickBy(Duration.ofSeconds(1))
        assertThat(provider(), equalTo(firstCreds.asHttp4k()))

        verify(exactly = 1) { containerCredentials(GetCredentials(relativePathUri)) }
    }

    @Test
    fun `gets credentials when expired time only`() {
        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)
        every { containerCredentials(GetCredentials(relativePathUri)) } returns Success(firstCreds)

        assertThat(provider(), equalTo(firstCreds.asHttp4k()))

        verify(exactly = 1) { containerCredentials(GetCredentials(relativePathUri)) }

        val secondCreds = credentialsExpiringAt(now.plusSeconds(61), 2)
        every { containerCredentials(GetCredentials(relativePathUri)) } returns Success(secondCreds)

        clock.tickBy(Duration.ofSeconds(1))
        assertThat(provider(), equalTo(firstCreds.asHttp4k()))

        verify(exactly = 1) { containerCredentials(GetCredentials(relativePathUri)) }

        clock.tickBy(Duration.ofMillis(1))
        assertThat(provider(), equalTo(secondCreds.asHttp4k()))

        verify(exactly = 2) { containerCredentials(GetCredentials(relativePathUri)) }
    }

    @Test
    fun `credentials chain gracefully fails outside container`() {
        val chain = CredentialsChain.ContainerCredentials(
            env = Environment.ENV,
            http = { Response(Status.INTERNAL_SERVER_ERROR) },
            clock = clock
        )

        assertThat(chain(), absent())
    }

    private fun credentialsExpiringAt(expiry: Instant, counter: Int) = Credentials(
        SessionToken.of("SessionToken"),
        AccessKeyId.of(counter.toString()),
        SecretAccessKey.of("SecretAccessKey"),
        Expiration.of(ZonedDateTime.ofInstant(expiry, ZoneId.of("UTC"))),
        ARN.of("arn:aws:sts:us-east-1:000000000001:role:myrole")
    )
}

class TestClock(private var time: Instant) : Clock() {
    override fun getZone(): ZoneId = TODO("Not yet implemented")

    override fun withZone(zone: ZoneId?): Clock = TODO("Not yet implemented")

    override fun instant(): Instant = time

    fun tickBy(duration: Duration) {
        time = time.plus(duration)
    }
}
