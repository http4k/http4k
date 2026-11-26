package sl4k

import com.natpryce.hamkrest.absent
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
private val secret = ChannelName.of("secret")
private val solo = ChannelName.of("solo")

interface MembershipContract {
    val driver: ChatDriver

    @Test
    fun `a private channel is only listed for its members, and others can't read or post in it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.createsPrivate(secret), equalTo(Verdict.Accepted))

        assertThat(ann.channels.list(), equalTo(listOf(general, random, secret)))
        assertThat(bob.channels.list(), equalTo(listOf(general, random)))
        assertThat(bob.channels.open(secret).canRead(), equalTo(false))
        assertThat(bob.channels.open(secret).posts("Let me in"), refusedAs(Verdict.Hidden))
    }

    @Test
    fun `a member invites a teammate into a private channel, who can then read and post in it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans for the offsite")

        assertThat(ann.channels.open(secret).invites(bob), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.list(), equalTo(listOf(general, random, secret))) }
        assertThat(bob.channels.open(secret).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, MessageText.of("Plans for the offsite"))))))
        assertThat(bob.channels.open(secret).posts("Count me in"), equalTo(Verdict.Accepted))
    }

    @Test
    fun `only members of a private channel can invite people into it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)
        ann.channels.createsPrivate(secret)

        assertThat(bob.channels.open(secret).invites(cat), refusedAs(Verdict.Hidden))
    }

    @Test
    fun `a public channel has nobody to invite, and can't be left`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).invites(bob), refusedAs(Verdict.Denied))
        assertThat(ann.channels.open(general).leaves(), refusedAs(Verdict.Denied))
    }

    @Test
    fun `a direct message is a private channel of two, listed under the other person's name`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)

        val dm = ann.channels.messagesDirectly(bob)
        ann.channels.open(dm).posts("Got a minute?")

        eventually { assertThat(bob.channels.directMessages(), equalTo(mapOf(ann.name to 1))) }
        assertThat(cat.channels.directMessages(), equalTo(emptyMap<String, Int>()))
        assertThat(cat.channels.open(dm).posts("Hi both"), refusedAs(Verdict.Hidden))
    }

    @Test
    fun `a member who leaves a private channel loses it, and can't read it any more`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)

        assertThat(bob.channels.open(secret).leaves(), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.list(), equalTo(listOf(general, random))) }
        assertThat(bob.channels.open(secret).canRead(), equalTo(false))
    }

    @Test
    fun `the owner removes a teammate who is watching, and they are told there's no such channel, and lose it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        bob.channels.open(secret)

        assertThat(ann.channels.open(secret).removes(bob), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.viewing(), absent()) }
        eventually { assertThat(bob.channels.list(), equalTo(listOf(general, random))) }
    }

    @Test
    fun `only the owner removes people, and a direct message can't be left`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        ann.channels.open(secret).invites(cat)
        val dm = ann.channels.messagesDirectly(bob)

        assertThat(bob.channels.open(secret).removes(cat), refusedAs(Verdict.Denied))
        assertThat(ann.channels.open(dm).leaves(), refusedAs(Verdict.Denied))
    }

    @Test
    fun `the last member leaving deletes the channel and everything in it`() {
        val ann = driver.teammate("ann")
        ann.channels.createsPrivate(solo)
        ann.channels.open(solo).posts("Mine")
        ann.channels.open(solo).topic.sets("Just me")
        ann.channels.open(solo).bookmarks.adds("Runbook", "https://example.com/runbook")
        ann.channels.open(solo).canvas.saves("Notes")

        assertThat(ann.channels.open(solo).leaves(), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.list(), equalTo(listOf(general, random))) }

        ann.channels.createsPrivate(solo)

        assertThat(ann.channels.open(solo).sees(), equalTo(ChannelSeen(emptyList())))
        assertThat(ann.channels.open(solo).topic.read(), equalTo(""))
        assertThat(ann.channels.open(solo).bookmarks.list(), equalTo(emptyList<Bookmarked>()))
        assertThat(ann.channels.open(solo).canvas.read(), equalTo(""))
    }
}
