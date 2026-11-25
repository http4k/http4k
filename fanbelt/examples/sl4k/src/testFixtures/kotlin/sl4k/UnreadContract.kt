package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.MessageText

private val general = ChannelName.of("general")
private val random = ChannelName.of("random")

interface UnreadContract {
    val driver: ChatDriver

    @Test
    fun `messages posted while you are in another channel show as unread`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(random)

        bob.channels.open(general).posts("Standup in five")
        bob.channels.open(general).posts("Actually, ten")

        eventually { assertThat(ann.channels.unread(), equalTo(mapOf(general to 2))) }
    }

    @Test
    fun `opening a channel reads it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(random)
        bob.channels.open(general).posts("Standup in five")
        eventually { assertThat(ann.channels.unread(), equalTo(mapOf(general to 1))) }

        ann.channels.open(general)

        eventually { assertThat(ann.channels.unread(), equalTo(emptyMap<ChannelName, Int>())) }
    }

    @Test
    fun `messages that arrive while you are in a channel are read when you leave it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general).posts("Standup in five")

        ann.channels.open(random)

        eventually { assertThat(ann.channels.unread(), equalTo(emptyMap<ChannelName, Int>())) }
    }

    @Test
    fun `messages that arrive after you leave a channel are unread`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        ann.channels.open(random)
        bob.channels.open(general)

        eventually(untilAClosedTabIsNoticed) {
            bob.channels.open(general).posts("Standup in five")
            assertThat(ann.channels.unread().keys, equalTo(setOf(general)))
        }
    }

    @Test
    fun `your own messages are never unread for you`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).posts("Notes from yesterday")

        ann.channels.open(random)

        eventually { assertThat(ann.channels.unread(), equalTo(emptyMap<ChannelName, Int>())) }
    }

    @Test
    fun `a divider sits above the first message you haven't read`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Morning")
        bob.channels.open(general)
        bob.channels.open(random)

        eventually(untilAClosedTabIsNoticed) {
            ann.channels.open(general).posts("Standup")
            assertThat(bob.channels.unread().keys, equalTo(setOf(general)))
        }
        ann.channels.open(general).posts("Lunch")

        assertThat(bob.channels.open(general).firstUnread(), equalTo("Standup"))
    }

    @Test
    fun `the divider is gone once you have read the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("Morning")
        eventually { assertThat(bob.channels.unread(), equalTo(mapOf(general to 1))) }

        bob.channels.open(general)

        eventually(untilAClosedTabIsNoticed) {
            bob.channels.open(random)
            assertThat(bob.channels.open(general).firstUnread(), equalTo(null))
        }
    }

    @Test
    fun `the divider skips a message deleted before you read it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("Wrong channel")
        ann.channels.open(general).posts("Morning")
        ann.channels.open(general).message(MessageText.of("Wrong channel")).deletes()
        eventually { assertThat(bob.channels.unread(), equalTo(mapOf(general to 1))) }

        assertThat(bob.channels.open(general).firstUnread(), equalTo("Morning"))
    }

    @Test
    fun `deleting a message someone hasn't read takes it off their unread count and their mentions`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("@${bob.handle} wrong person")
        ann.channels.open(general).posts("Morning all")
        eventually { assertThat(bob.channels.unread(), equalTo(mapOf(general to 2))) }
        assertThat(bob.channels.mentions(), equalTo(mapOf(general to 1)))

        ann.channels.open(general).message(MessageText.of("@${bob.handle} wrong person")).deletes()

        eventually { assertThat(bob.channels.unread(), equalTo(mapOf(general to 1))) }
        assertThat(bob.channels.mentions(), equalTo(emptyMap<ChannelName, Int>()))
    }
}
