package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName

private val general = ChannelName.of("general")
private val random = ChannelName.of("random")
private val design = ChannelName.of("design")

interface ChannelsContract {
    val driver: ChatDriver

    @Test
    fun `a channel one teammate creates shows in everyone's sidebar`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.creates("design"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.list(), equalTo(listOf(design, general, random))) }
    }

    @Test
    fun `a channel name that is taken or malformed is refused`() {
        val ann = driver.teammate("ann")

        assertThat(ann.channels.creates("general"), equalTo(Verdict.Rejected("#general already exists")))
        assertThat(ann.channels.creates("Design Team"), equalTo(Verdict.Rejected("Use lowercase letters, numbers and dashes")))
        assertThat(ann.channels.creates("dm-ann-bob"), equalTo(Verdict.Rejected("Names starting dm- are for direct messages")))
    }

    @Test
    fun `a teammate who has never opened a channel sees the open channels, with nothing unread`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).posts("Morning")
        val bob = driver.teammate("bob")

        assertThat(bob.channels.list(), equalTo(listOf(general, random)))
        assertThat(bob.channels.unread(), equalTo(emptyMap<ChannelName, Int>()))
    }

    @Test
    fun `a channel is headed with its name, and a direct message with the other person's`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        val dm = ann.channels.messagesDirectly(bob)

        assertThat(ann.channels.open(general).heading(), equalTo("#general"))
        assertThat(ann.channels.open(dm).heading(), equalTo(bob.name))
        assertThat(bob.channels.open(dm).heading(), equalTo(ann.name))
    }
}
