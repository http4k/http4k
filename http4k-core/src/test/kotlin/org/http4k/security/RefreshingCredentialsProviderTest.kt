package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class RefreshingCredentialsProviderTest {

    private val now = Instant.now()
    private val clock = TestClock(now)

    @Test
    fun `gets credentials first time only`() {
        var calls = 0

        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)

        val provider = CredentialsProvider.Refreshing<String>(Duration.ofSeconds(60), clock) {
            calls++
            firstCreds
        }

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

        val provider = CredentialsProvider.Refreshing<String>(Duration.ofSeconds(60), clock) {
            calls++
            toReturn
        }

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

    @Test
    fun `on failure to refresh returns previous creds and then throws`() {
        var calls = 0

        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)

        val e = Exception()
        val provider = CredentialsProvider.Refreshing<String>(Duration.ofSeconds(60), clock) {
            when {
                calls++ == 0 -> firstCreds
                else -> throw e
            }
        }

        assertThat(provider(), equalTo(firstCreds.credentials))

        clock.tickBy(Duration.ofSeconds(61))

        assertThat(provider(), equalTo(firstCreds.credentials))

        clock.tickBy(Duration.ofSeconds(60))

        assertThat({provider() }, throws(equalTo(e)))
    }

    @Test
    fun `uses almost-stale credentials when there is more than half of the grace period left`() {
        var completedCalls = 0

        val firstCreds = credentialsExpiringAt(now.plusSeconds(11), 1)

        var latestCreds = firstCreds

        var mainRefreshLock = CountDownLatch(1)

        val provider = CredentialsProvider.Refreshing<String>(Duration.ofSeconds(10), clock) {
            mainRefreshLock.await()

            completedCalls++

            latestCreds
        }

        mainRefreshLock.countDown()

        // gets first creds when null
        assertThat(provider(), equalTo(firstCreds.credentials))
        assertThat(completedCalls, equalTo(1))

        // doesn't refresh
        assertThat(provider(), equalTo(firstCreds.credentials))
        assertThat(completedCalls, equalTo(1))

        mainRefreshLock = CountDownLatch(1)

        // we are now into refresh period
        clock.tickBy(Duration.ofSeconds(1))

        val refreshed = AtomicReference<String>(null)

        val secondCreds = credentialsExpiringAt(now.plusSeconds(11), 1)
        latestCreds = secondCreds

        val firstRefreshLock = CountDownLatch(1)

        // refresh operation starts
        thread {
            refreshed.set(provider())
            firstRefreshLock.countDown()
        }

        // whilst we are refreshing, use old credentials
        assertThat(provider(), equalTo(firstCreds.credentials))
        assertThat(completedCalls, equalTo(1))

        mainRefreshLock.countDown()
        firstRefreshLock.await()

        // whilst we now we should have new credentials
        assertThat(refreshed.get(), equalTo(secondCreds.credentials))

        // get new credentials again as not expired
        assertThat(provider(), equalTo(secondCreds.credentials))
        assertThat(completedCalls, equalTo(1))

        // we now are too close to the refresh period
        clock.tickBy(Duration.ofSeconds(7))

        val thirdCreds = credentialsExpiringAt(now.plusSeconds(11), 1)
        latestCreds = thirdCreds

        val secondRefreshLock = CountDownLatch(1)

        val secondRefreshed = AtomicReference<String>(null)

        mainRefreshLock = CountDownLatch(1)

        // refresh operation starts
        thread {
            secondRefreshed.set(provider())
            secondRefreshLock.countDown()
        }

        val chasingRefreshLock = CountDownLatch(1)

        val chasingRefresh = AtomicReference<String>(null)

        // chasing (cached) refresh
        thread {
            chasingRefresh.set(provider())
            chasingRefreshLock.countDown()
        }

        mainRefreshLock.countDown()
        secondRefreshLock.await()

        // whilst we now we should have new credentials
        assertThat(secondRefreshed.get(), equalTo(thirdCreds.credentials))
        assertThat(completedCalls, equalTo(2))

        // and the chasing one also has that new value as well
        chasingRefreshLock.await()
        assertThat(chasingRefresh.get(), equalTo(thirdCreds.credentials))
        assertThat(completedCalls, equalTo(3))
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
