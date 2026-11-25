package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.MessageText

private val general = ChannelName.of("general")
private val random = ChannelName.of("random")
private val secret = ChannelName.of("secret")
private val plans = MessageText.of("Plans")
private val shippedIt = MessageText.of("Shipped it")

interface PinsContract {
    val driver: ChatDriver

    @Test
    fun `a pinned message shows at the top for everyone until someone unpins it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).posts("Morning all")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).message(shippedIt).togglesPin(), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).pinned(), equalTo(listOf(Pinned(ann.name, "Shipped it")))) }
        assertThat(bob.channels.open(random).pinned(), equalTo(emptyList<Pinned>()))

        assertThat(bob.channels.open(general).message(shippedIt).togglesPin(), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.open(general).pinned(), equalTo(emptyList<Pinned>())) }
    }

    @Test
    fun `clicking the pin at the top of the channel unpins the message for everyone`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).togglesPin()
        ann.channels.open(general)

        assertThat(bob.channels.open(general).message(shippedIt).unpinsFromTheTop(), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.open(general).pinned(), equalTo(emptyList<Pinned>())) }
    }

    @Test
    fun `a pin follows its message's edits, and goes when the message is deleted`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).togglesPin()
        bob.channels.open(general)

        ann.channels.open(general).message(shippedIt).edits("Shipped it twice")

        eventually { assertThat(bob.channels.open(general).pinned(), equalTo(listOf(Pinned(ann.name, "Shipped it twice")))) }

        ann.channels.open(general).message(MessageText.of("Shipped it twice")).deletes()

        eventually { assertThat(bob.channels.open(general).pinned(), equalTo(emptyList<Pinned>())) }
    }

    @Test
    fun `pinning where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans")

        assertThat(bob.channels.open(secret).message(plans).togglesPin(), equalTo(Verdict.Hidden))
    }
}
