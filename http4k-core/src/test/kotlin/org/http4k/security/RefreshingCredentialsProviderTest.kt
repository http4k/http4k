package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class RefreshingCredentialsProviderTest {

    private val now = Instant.now()
    private val clock = TestClock(now)

    @Test
    fun `gets credentials first time only`() {
        var calls = 0

        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)

        val provider = CredentialsProvider.Refreshing({
            calls++
            firstCreds
        }, Duration.ofSeconds(60), clock)

        assertThat(provider(), equalTo(firstCreds.credentials))
        clock.tickBy(Duration.ofSeconds(1))
        assertThat(provider(), equalTo(firstCreds.credentials))
        assertThat(calls, equalTo(1))
    }

    @Test
    fun `gets credentials when expired time only`() {
        var calls = 0

        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)

        var toReturn = firstCreds

        val provider = CredentialsProvider.Refreshing({
            calls++
            toReturn
        }, Duration.ofSeconds(60), clock)

        assertThat(provider(), equalTo(firstCreds.credentials))
        assertThat(calls, equalTo(1))

        clock.tickBy(Duration.ofSeconds(1))

        assertThat(provider(), equalTo(firstCreds.credentials))
        assertThat(calls, equalTo(1))

        clock.tickBy(Duration.ofSeconds(60))

        val secondCreds = credentialsExpiringAt(now.plusSeconds(61), 2)

        toReturn = secondCreds

        assertThat(provider(), equalTo(secondCreds.credentials))

        assertThat(calls, equalTo(2))
    }

    private fun credentialsExpiringAt(expiry: Instant, counter: Int) = ExpiringCredentials(
        "token$counter", expiry
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
