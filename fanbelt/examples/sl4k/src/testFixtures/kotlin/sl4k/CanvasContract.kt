package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName

private val general = ChannelName.of("general")
private val random = ChannelName.of("random")
private val secret = ChannelName.of("secret")

interface CanvasContract {
    val driver: ChatDriver

    @Test
    fun `a saved canvas shows for everyone in the channel, and only in that channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).canvas.saves("Release checklist\n- tag\n- deploy"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).canvas.read(), equalTo("Release checklist\n- tag\n- deploy")) }
        assertThat(bob.channels.open(random).canvas.read(), equalTo(""))
    }

    @Test
    fun `a teammate picks up where the last save left off`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).canvas.saves("First draft")

        assertThat(bob.channels.open(general).canvas.saves("Second draft"), equalTo(Verdict.Accepted))
        assertThat(ann.channels.open(general).canvas.saves("Third draft"), equalTo(Verdict.Accepted))

        assertThat(bob.channels.open(general).canvas.read(), equalTo("Third draft"))
    }

    @Test
    fun `saving over a teammate's newer save is refused, and their text survives`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).canvas.saves("First draft")
        ann.channels.open(general).canvas.begins()
        bob.channels.open(general).canvas.saves("Bob's draft")

        assertThat(ann.channels.open(general).canvas.saves("Ann's draft"), equalTo(Verdict.Rejected("A teammate saved first. Copy your text, then edit again")))
        assertThat(bob.channels.open(general).canvas.read(), equalTo("Bob's draft"))

        ann.channels.open(general).canvas.begins()

        assertThat(ann.channels.open(general).canvas.saves("Ann's draft"), equalTo(Verdict.Accepted))
        assertThat(bob.channels.open(general).canvas.read(), equalTo("Ann's draft"))
    }

    @Test
    fun `a blank canvas clears it, and an enormous one is refused`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).canvas.saves("First draft")

        assertThat(ann.channels.open(general).canvas.saves("x".repeat(20001)), equalTo(Verdict.Rejected("Keep the canvas to 20000 characters")))
        assertThat(ann.channels.open(general).canvas.read(), equalTo("First draft"))

        ann.channels.open(general).canvas.begins()

        assertThat(ann.channels.open(general).canvas.saves(""), equalTo(Verdict.Accepted))
        assertThat(ann.channels.open(general).canvas.read(), equalTo(""))
    }

    @Test
    fun `saving a canvas where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)

        assertThat(bob.channels.open(secret).canvas.saves("Peeking"), equalTo(Verdict.Hidden))
    }
}
