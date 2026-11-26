package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.refusedAs
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.MessageText

private val general = ChannelName.of("general")
private val secret = ChannelName.of("secret")
private val lunch = MessageText.of("Lunch?")
private val tacos = MessageText.of("Tacos")
private val plans = MessageText.of("Plans")

interface ThreadsContract {
    val driver: ChatDriver

    @Test
    fun `replies show in the thread for everyone looking at it, in order, and not in the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general).message(lunch).opensThread()

        assertThat(ann.channels.open(general).message(lunch).replies("Tacos"), equalTo(Verdict.Accepted))
        assertThat(bob.channels.open(general).message(lunch).replies("Seconded"), equalTo(Verdict.Accepted))

        eventually {
            assertThat(bob.channels.open(general).message(lunch).thread(), equalTo(ThreadSeen(Said(ann.name, lunch, replies = 2), listOf(Said(ann.name, tacos), Said(bob.name, MessageText.of("Seconded"))))))
        }
        assertThat(ann.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, lunch, replies = 2))))
    }

    @Test
    fun `everyone in the channel sees how many replies a message has`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general)

        ann.channels.open(general).message(lunch).replies("Tacos")

        eventually { assertThat(bob.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, lunch, replies = 1)))) }
    }

    @Test
    fun `replying to a reply, with nothing to say, or where you can't see the channel is refused`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.open(general).posts("Lunch?")
        ann.channels.open(general).message(lunch).replies("Tacos")
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans")

        assertThat(bob.channels.open(general).message(tacos).replies("Burritos"), equalTo(Verdict.Rejected("Threads are one level deep")))
        assertThat(bob.channels.open(general).message(lunch).replies("   "), equalTo(Verdict.Rejected("Say something first")))
        assertThat(bob.channels.open(secret).message(plans).replies("Me too"), refusedAs(Verdict.Hidden))
    }
}
