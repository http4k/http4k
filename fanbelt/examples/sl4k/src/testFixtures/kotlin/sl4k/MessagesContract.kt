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
private val random = ChannelName.of("random")
private val nowhere = ChannelName.of("nowhere")
private val morningAll = MessageText.of("Morning all")
private val lunch = MessageText.of("Lunch?")
private val tacos = MessageText.of("Tacos")
private val mine = MessageText.of("Mine")

interface MessagesContract {
    val driver: ChatDriver

    @Test
    fun `a message posted in a channel shows for everyone in it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).posts("Morning all"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, morningAll))))) }
    }

    @Test
    fun `a message stays in its own channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)

        ann.channels.open(general).posts("Morning all")

        assertThat(bob.channels.open(random).sees(), equalTo(ChannelSeen(emptyList())))
    }

    @Test
    fun `a blank message is refused`() {
        assertThat(driver.teammate("ann").channels.open(general).posts("   "), equalTo(Verdict.Rejected("Say something first")))
    }

    @Test
    fun `posting to a channel that doesn't exist is refused`() {
        assertThat(driver.teammate("ann").channels.open(nowhere).posts("Hello?"), refusedAs(Verdict.Hidden))
    }

    @Test
    fun `a channel shows its latest fifty messages, and older ones on request`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        (1..55).forEach { ann.channels.open(general).posts("message $it") }

        assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen((6..55).map { Said(ann.name, MessageText.of("message $it")) }, older = true)))

        bob.channels.open(general).showsOlder()

        assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen((1..55).map { Said(ann.name, MessageText.of("message $it")) }, older = false)))
    }

    @Test
    fun `an author's edit shows for everyone, marked as edited`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch at 12")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).message(MessageText.of("Lunch at 12")).edits(to = "Lunch at 1"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, MessageText.of("Lunch at 1"), edited = true)))) }
    }

    @Test
    fun `a deleted message leaves everyone's history`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Wrong channel, sorry")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).message(MessageText.of("Wrong channel, sorry")).deletes(), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen(emptyList()))) }
    }

    @Test
    fun `only the author can edit or delete a message, and an edit can't blank it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.open(general).posts("Mine")

        assertThat(bob.channels.open(general).message(mine).edits(to = "Yours"), refusedAs(Verdict.Denied))
        assertThat(bob.channels.open(general).message(mine).deletes(), refusedAs(Verdict.Denied))
        assertThat(ann.channels.open(general).message(mine).edits(to = "   "), equalTo(Verdict.Rejected("Say something first")))
    }

    @Test
    fun `a deleted message with replies stays, so its thread still makes sense`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general).message(lunch).replies("Tacos")

        ann.channels.open(general).message(lunch).deletes()

        val tombstone = Said(ann.name, MessageText.of("This message was deleted"), replies = 1, deleted = true)
        eventually { assertThat(bob.channels.open(general).sees().messages, equalTo(listOf(tombstone))) }
        assertThat(bob.channels.open(general).message(lunch).thread(), equalTo(ThreadSeen(tombstone, listOf(Said(bob.name, tacos)))))
    }
}
