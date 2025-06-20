package org.http4k.connect.amazon.instancemetadata

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.instancemetadata.model.Token
import org.http4k.core.Method.PUT
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.util.TickingClock
import org.junit.jupiter.api.Test
import java.time.Duration

class Ec2InstanceMetadataTokenProviderTest {

    private var nextToken = 1
    private val fakeImds = "latest/api/token" bind PUT to {
        Response(OK).body("token${nextToken++}")
    }

    @Test
    fun `static token provider caches token`() {
        val provider = staticEc2InstanceMetadataTokenProvider(http = fakeImds)

        assertThat(provider(), equalTo(Token.parse("token1")))
        assertThat(provider(), equalTo(Token.parse("token1")))
        assertThat(nextToken, equalTo(2))
    }

    @Test
    fun `refreshing token provider caches token`() {
        val provider = refreshingEc2InstanceMetadataTokenProvider(http = fakeImds)

        assertThat(provider(), equalTo(Token.parse("token1")))
        assertThat(provider(), equalTo(Token.parse("token1")))
        assertThat(nextToken, equalTo(2))
    }

    @Test
    fun `refreshing token provider refreshes after token expires`() {
        val clock = TickingClock()
        val provider = refreshingEc2InstanceMetadataTokenProvider(
            clock,
            tokenTtl = Duration.ofMinutes(5),
            http = fakeImds
        )

        assertThat(provider(), equalTo(Token.parse("token1")))
        assertThat(provider(), equalTo(Token.parse("token1")))

        clock.tick(Duration.ofMinutes(6))
        assertThat(provider(), equalTo(Token.parse("token2")))
        assertThat(provider(), equalTo(Token.parse("token2")))
    }

    @Test
    fun `refreshing token provider refreshes after grace period`() {
        val clock = TickingClock()
        val provider = refreshingEc2InstanceMetadataTokenProvider(
            clock,
            tokenTtl = Duration.ofMinutes(10),
            gracePeriod = Duration.ofMinutes(2),
            http = fakeImds
        )

        assertThat(provider(), equalTo(Token.parse("token1")))
        assertThat(provider(), equalTo(Token.parse("token1")))

        clock.tick(Duration.ofMinutes(9))
        assertThat(provider(), equalTo(Token.parse("token2")))
        assertThat(provider(), equalTo(Token.parse("token2")))
    }
}
