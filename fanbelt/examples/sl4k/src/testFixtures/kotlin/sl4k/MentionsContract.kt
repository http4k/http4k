package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.MessageText

private val general = ChannelName.of("general")
private val random = ChannelName.of("random")
private val secret = ChannelName.of("secret")

interface MentionsContract {
    val driver: ChatDriver

    @Test
    fun `a mention names the teammate in the message`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("@${bob.handle} can you look at this?")

        eventually { assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, MessageText.of("@${bob.handle} can you look at this?"), setOf(bob.name)))))) }
    }

    @Test
    fun `a mention still names the teammate inside bold text`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("*ask @${bob.handle} first*")

        eventually { assertThat(bob.channels.open(general).sees().messages.single().mentions, equalTo(setOf(bob.name))) }
    }

    @Test
    fun `an @ that matches nobody who can see the channel is just text`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)

        ann.channels.open(general).posts("@nobody-here hello")
        ann.channels.open(secret).posts("@${bob.handle} you can't see this")

        assertThat(ann.channels.open(general).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, MessageText.of("@nobody-here hello"))))))
        assertThat(ann.channels.open(secret).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, MessageText.of("@${bob.handle} you can't see this"))))))
    }

    @Test
    fun `unread mentions are counted in the sidebar until the channel is read`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)

        ann.channels.open(general).posts("Morning all")
        ann.channels.open(general).posts("@${bob.handle} can you look at this?")

        eventually { assertThat(bob.channels.mentions(), equalTo(mapOf(general to 1))) }
        assertThat(bob.channels.unread(), equalTo(mapOf(general to 2)))

        bob.channels.open(general)

        eventually { assertThat(bob.channels.mentions(), equalTo(emptyMap<ChannelName, Int>())) }
    }

    @Test
    fun `typing @ suggests the other teammates who can see the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)

        assertThat(ann.channels.open(secret).suggestions("thoughts @"), equalTo(setOf(bob.name)))
        assertThat(ann.channels.open(general).suggestions("thoughts @"), equalTo(setOf(bob.name, cat.name)))
        assertThat(ann.channels.open(general).suggestions("thoughts @${cat.handle.take(3)}"), equalTo(setOf(cat.name)))
    }

    @Test
    fun `tab completes the handle being typed`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)

        assertThat(ann.channels.open(secret).tabs("thoughts @${bob.handle.take(2)}"), equalTo("thoughts @${bob.handle} "))
    }

    @Test
    fun `editing a mention in counts it for the teammate mentioned`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("Have a look")

        ann.channels.open(general).message(MessageText.of("Have a look")).edits(to = "@${bob.handle} have a look")

        eventually { assertThat(bob.channels.mentions(), equalTo(mapOf(general to 1))) }
    }
}
