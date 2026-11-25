package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.MessageText
import java.time.Duration

private val general = ChannelName.of("general")
private val random = ChannelName.of("random")
private val secret = ChannelName.of("secret")
private val lunch = MessageText.of("Lunch?")
private val tacos = MessageText.of("Tacos")

interface SearchContract {
    val driver: ChatDriver

    @Test
    fun `search finds messages across channels, newest first, whatever the case`() {
        val ann = driver.teammate("ann")
        val random = ChannelName.of("random")
        ann.channels.open(general).posts("Lunch at noon?")
        driver.tick(Duration.ofSeconds(1))
        ann.channels.open(random).posts("who wants lunch")
        ann.channels.open(random).posts("something else")

        assertThat(ann.channels.searches("LUNCH"), equalTo(listOf(Found("#random", ann.name, MessageText.of("who wants lunch")), Found("#general", ann.name, MessageText.of("Lunch at noon?")))))
    }

    @Test
    fun `search finds replies in threads`() {
        val ann = driver.teammate("ann")
        val lunch = MessageText.of("Lunch?")
        ann.channels.open(general).posts(lunch.value)
        ann.channels.open(general).message(lunch).replies("Tacos please")

        assertThat(ann.channels.searches("tacos").map { it.text }, equalTo(listOf(MessageText.of("Tacos please"))))
    }

    @Test
    fun `search only looks in channels you can see`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("the launch date")
        val dm = ann.channels.messagesDirectly(bob)
        ann.channels.open(dm).posts("launch is close")

        assertThat(ann.channels.searches("launch").map { it.text }.sortedBy { it.value }, equalTo(listOf(MessageText.of("launch is close"), MessageText.of("the launch date"))))
        assertThat(bob.channels.searches("launch"), equalTo(listOf(Found(ann.name, ann.name, MessageText.of("launch is close")))))
    }

    @Test
    fun `search results stay live`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        assertThat(bob.channels.searches("lunch"), equalTo(emptyList<Found>()))

        ann.channels.open(general).posts("Lunch?")
        eventually { assertThat(bob.channels.searches("lunch").map { it.text }, equalTo(listOf(MessageText.of("Lunch?")))) }

        ann.channels.open(general).message(MessageText.of("Lunch?")).edits("Dinner?")
        eventually { assertThat(bob.channels.searches("lunch"), equalTo(emptyList<Found>())) }
    }

    @Test
    fun `being removed from a channel takes its messages out of your results while you watch`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        ann.channels.open(secret).posts("the launch date")
        assertThat(bob.channels.searches("launch").size, equalTo(1))

        ann.channels.open(secret).removes(bob)

        eventually { assertThat(bob.channels.searches("launch"), equalTo(emptyList<Found>())) }
    }

    @Test
    fun `search doesn't find deleted messages, and a blank search finds nothing`() {
        val ann = driver.teammate("ann")
        val lunch = MessageText.of("Lunch?")
        ann.channels.open(general).posts(lunch.value)
        ann.channels.open(general).message(lunch).deletes()

        assertThat(ann.channels.searches("lunch"), equalTo(emptyList<Found>()))
        ann.channels.open(general).posts("Lunch again?")
        assertThat(ann.channels.searches(""), equalTo(emptyList<Found>()))
    }
}
