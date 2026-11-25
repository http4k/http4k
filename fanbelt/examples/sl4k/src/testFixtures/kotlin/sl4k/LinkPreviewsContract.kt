package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.MessageText
import java.time.Duration

private val general = ChannelName.of("general")

interface LinkPreviewsContract {
    val driver: ChatDriver

    @Test
    fun `a link's preview appears for everyone watching, after the message`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("read https://example.test/article please")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(bob.channels.open(general).sees().messages.single().preview, equalTo(LinkPreview("An article", "All about it"))) }
    }

    @Test
    fun `each link in a message gets its own preview, in order`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/plain and https://example.test/article")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(ann.channels.open(general).sees().messages.single().previews, equalTo(listOf(LinkPreview("A plain page", "Nothing fancy"), LinkPreview("An article", "All about it")))) }
    }

    @Test
    fun `a preview shows the page's image from our own server`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/pictured")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(ann.channels.open(general).sees().messages.single().preview, equalTo(LinkPreview("Pictured", "", true))) }
    }

    @Test
    fun `a page with no open graph tags previews its title and description`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/plain")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(ann.channels.open(general).sees().messages.single().preview, equalTo(LinkPreview("A plain page", "Nothing fancy"))) }
    }

    @Test
    fun `a preview follows a redirect`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/moved")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(ann.channels.open(general).sees().messages.single().preview, equalTo(LinkPreview("An article", "All about it"))) }
    }

    @Test
    fun `editing a link away removes its preview, and editing one in adds it`() {
        val ann = driver.teammate("ann").channels.open(general)
        ann.posts("https://example.test/article")
        driver.tick(Duration.ofSeconds(2))
        eventually { assertThat(ann.sees().messages.single().preview?.title, equalTo("An article")) }

        ann.message(MessageText.of("https://example.test/article")).edits("https://example.test/plain")
        driver.tick(Duration.ofSeconds(2))
        eventually { assertThat(ann.sees().messages.single().preview?.title, equalTo("A plain page")) }

        ann.message(MessageText.of("https://example.test/plain")).edits("no link now")
        eventually { assertThat(ann.sees().messages.single().preview, equalTo(null)) }
    }

    @Test
    fun `deleting a message removes its preview`() {
        val ann = driver.teammate("ann").channels.open(general)
        val link = MessageText.of("https://example.test/article")
        ann.posts(link.value)
        driver.tick(Duration.ofSeconds(2))
        eventually { assertThat(ann.sees().messages.single().preview?.title, equalTo("An article")) }

        ann.message(link).replies("kept alive by this reply")
        ann.message(link).deletes()

        eventually { assertThat(ann.sees().messages.single().preview, equalTo(null)) }
    }
}
