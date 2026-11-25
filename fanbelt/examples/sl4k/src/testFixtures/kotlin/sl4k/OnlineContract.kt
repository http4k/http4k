package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import java.time.Duration

private val general = ChannelName.of("general")
private val random = ChannelName.of("random")

interface OnlineContract {
    val driver: ChatDriver

    @Test
    fun `everyone sees who is online, whichever channel they are in`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(random)

        eventually { assertThat(ann.online(), equalTo(setOf(ann.name, bob.name))) }

        bob.closes()

        eventually(untilAClosedTabIsNoticed) { assertThat(ann.online(), equalTo(setOf(ann.name))) }
    }

    @Test
    fun `being connected is being online, on any page`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.searches("anything")

        eventually { assertThat(ann.online(), equalTo(setOf(ann.name, bob.name))) }
    }

    @Test
    fun `leaving a channel for another page leaves the channel but stays online`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val random = ChannelName.of("random")
        ann.channels.open(random)
        bob.channels.open(random)
        eventually { assertThat(ann.online(), equalTo(setOf(ann.name, bob.name))) }

        bob.channels.searches("anything")

        eventually(untilAClosedTabIsNoticed) {
            ann.channels.open(random).posts("Still there?")
            assertThat(bob.channels.unread().keys, equalTo(setOf(random)))
        }
        assertThat(ann.online(), equalTo(setOf(ann.name, bob.name)))
    }

    @Test
    fun `teammates see who is typing, until they stop for five seconds`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general)

        bob.channels.open(general).types("Hello th")

        eventually { assertThat(ann.channels.open(general).typing(), equalTo(setOf(bob.name))) }
        assertThat(bob.channels.open(general).typing(), equalTo(emptySet<String>()))

        driver.tick(Duration.ofSeconds(6))

        eventually { assertThat(ann.channels.open(general).typing(), equalTo(emptySet<String>())) }
    }

    @Test
    fun `a teammate stops showing as typing as soon as they send`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general).types("Hello there")
        eventually { assertThat(ann.channels.open(general).typing(), equalTo(setOf(bob.name))) }

        assertThat(bob.channels.open(general).posts("Hello there"), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.open(general).typing(), equalTo(emptySet<String>())) }
    }

    @Test
    fun `each teammate sees which person in the list is them`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general)

        eventually { assertThat(ann.you(), equalTo(ann.name)) }
        eventually { assertThat(bob.you(), equalTo(bob.name)) }
    }
}
