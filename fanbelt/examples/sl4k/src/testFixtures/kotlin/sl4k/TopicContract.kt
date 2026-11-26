package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.refusedAs
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName

private val general = ChannelName.of("general")
private val random = ChannelName.of("random")
private val secret = ChannelName.of("secret")

interface TopicContract {
    val driver: ChatDriver

    @Test
    fun `a channel's topic shows for everyone in it, and only in that channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).topic.sets("Shipping on Friday"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).topic.read(), equalTo("Shipping on Friday")) }
        assertThat(bob.channels.open(random).topic.read(), equalTo(""))
    }

    @Test
    fun `a blank topic clears it, and a long one is refused`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).topic.sets("Shipping on Friday")

        assertThat(ann.channels.open(general).topic.sets("x".repeat(251)), equalTo(Verdict.Rejected("Keep the topic to 250 characters")))
        assertThat(ann.channels.open(general).topic.read(), equalTo("Shipping on Friday"))

        assertThat(ann.channels.open(general).topic.sets("   "), equalTo(Verdict.Accepted))
        assertThat(ann.channels.open(general).topic.read(), equalTo(""))
    }

    @Test
    fun `setting the topic where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)

        assertThat(bob.channels.open(secret).topic.sets("Peeking"), refusedAs(Verdict.Hidden))
    }
}
