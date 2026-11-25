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

interface BookmarksContract {
    val driver: ChatDriver

    @Test
    fun `bookmarks show for everyone in the channel, in the order they were added`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).bookmarks.adds("Runbook", "https://example.com/runbook"), equalTo(Verdict.Accepted))
        assertThat(ann.channels.open(general).bookmarks.adds("Dashboard", "http://example.com/dash?team=acme&view=1"), equalTo(Verdict.Accepted))

        eventually {
            assertThat(bob.channels.open(general).bookmarks.list(), equalTo(listOf(Bookmarked("Runbook", "https://example.com/runbook"), Bookmarked("Dashboard", "http://example.com/dash?team=acme&view=1"))))
        }
        assertThat(bob.channels.open(random).bookmarks.list(), equalTo(emptyList<Bookmarked>()))
    }

    @Test
    fun `anyone in the channel can remove a bookmark`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).bookmarks.adds("Runbook", "https://example.com/runbook")
        ann.channels.open(general)

        assertThat(bob.channels.open(general).bookmarks.removes("Runbook"), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.open(general).bookmarks.list(), equalTo(emptyList<Bookmarked>())) }
    }

    @Test
    fun `a bookmark needs a title and a web link`() {
        val ann = driver.teammate("ann")

        assertThat(ann.channels.open(general).bookmarks.adds("  ", "example.com"), equalTo(Verdict.Rejected("Give the bookmark a title")))
        assertThat(ann.channels.open(general).bookmarks.adds("Sneaky", "javascript:alert(1)"), equalTo(Verdict.Rejected("Use an http or https link")))
        assertThat(ann.channels.open(general).bookmarks.adds("Files", "ftp://example.com/x"), equalTo(Verdict.Rejected("Use an http or https link")))
        assertThat(ann.channels.open(general).bookmarks.list(), equalTo(emptyList<Bookmarked>()))
    }

    @Test
    fun `changing bookmarks where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).bookmarks.adds("Plans", "https://example.com/plans")

        assertThat(bob.channels.open(secret).bookmarks.adds("Peeking", "https://example.com/peek"), equalTo(Verdict.Hidden))
        assertThat(bob.channels.open(secret).bookmarks.removes("Plans"), equalTo(Verdict.Hidden))
    }
}
