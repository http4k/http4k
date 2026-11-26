package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.refusedAs
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Emoji
import sl4k.domain.MessageText

private val general = ChannelName.of("general")
private val secret = ChannelName.of("secret")
private val lunch = MessageText.of("Lunch?")
private val tacos = MessageText.of("Tacos")
private val plans = MessageText.of("Plans")
private val shippedIt = MessageText.of("Shipped it")
private val mine = MessageText.of("Mine")

interface ReactionsContract {
    val driver: ChatDriver

    @Test
    fun `a reaction shows its count for everyone in the channel, marked as yours only for you`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).message(shippedIt).reacts(Emoji.tada), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1))))) }
        assertThat(ann.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1), mine = setOf(Emoji.tada)))))
    }

    @Test
    fun `the same reaction from two teammates counts twice, and reacting again takes yours back`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).reacts(Emoji.tada)
        bob.channels.open(general).message(shippedIt).reacts(Emoji.tada)
        eventually { assertThat(ann.channels.open(general).sees().messages.single().reactions, equalTo(mapOf(Emoji.tada to 2))) }

        bob.channels.open(general).message(shippedIt).reacts(Emoji.tada)

        eventually { assertThat(ann.channels.open(general).sees().messages.single(), equalTo(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1), mine = setOf(Emoji.tada)))) }
        assertThat(bob.channels.open(general).sees().messages.single(), equalTo(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1))))
    }

    @Test
    fun `replies in a thread take reactions too`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        ann.channels.open(general).message(lunch).replies("Tacos")
        bob.channels.open(general).message(lunch).opensThread()

        ann.channels.open(general).message(tacos).reacts(Emoji.heart)

        eventually { assertThat(bob.channels.open(general).message(lunch).thread().replies, equalTo(listOf(Said(ann.name, tacos, reactions = mapOf(Emoji.heart to 1))))) }
    }

    @Test
    fun `reacting where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans")

        assertThat(bob.channels.open(secret).message(plans).reacts(Emoji.eyes), refusedAs(Verdict.Hidden))
    }
}
