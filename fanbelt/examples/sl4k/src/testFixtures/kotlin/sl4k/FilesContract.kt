package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.model.Base64Blob
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.MessageText

private val general = ChannelName.of("general")
private val secret = ChannelName.of("secret")
private val plans = MessageText.of("Plans")
private val shippedIt = MessageText.of("Shipped it")
private val mine = MessageText.of("Mine")
private val notes = FileToSend("notes.txt", "text/plain", Base64Blob.encode("Remember the milk"))
private val photo = FileToSend("photo.png", "image/png", Base64Blob.encode(byteArrayOf(-119, 80, 78, 71)))

interface FilesContract {
    val driver: ChatDriver

    @Test
    fun `a file sent to a channel shows for everyone and downloads as it was sent`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).attaches(notes, "Shipped it"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).message(shippedIt).attachments(), equalTo(listOf(Attached("notes.txt", inline = false)))) }
        assertThat(bob.channels.open(general).downloads("notes.txt"), equalTo(Downloaded(notes.contents, "application/octet-stream", inline = false)))
    }

    @Test
    fun `an image shows inline, and a file with nothing to say goes under its own name`() {
        val ann = driver.teammate("ann")

        assertThat(ann.channels.open(general).attaches(photo), equalTo(Verdict.Accepted))

        assertThat(ann.channels.open(general).message(MessageText.of("photo.png")).attachments(), equalTo(listOf(Attached("photo.png", inline = true))))
        assertThat(ann.channels.open(general).downloads("photo.png"), equalTo(Downloaded(photo.contents, "image/png", inline = true)))
    }

    @Test
    fun `a file that could run in the page never shows inline, whatever it claims to be`() {
        val ann = driver.teammate("ann")
        val script = Base64Blob.encode("<script>alert(1)</script>")

        ann.channels.open(general).attaches(FileToSend("page.html", "text/html", script), "Plans")
        ann.channels.open(general).attaches(FileToSend("logo.svg", "image/svg+xml", script), "Mine")

        assertThat(ann.channels.open(general).message(plans).attachments(), equalTo(listOf(Attached("page.html", inline = false))))
        assertThat(ann.channels.open(general).message(mine).attachments(), equalTo(listOf(Attached("logo.svg", inline = false))))
        assertThat(ann.channels.open(general).downloads("logo.svg"), equalTo(Downloaded(script, "application/octet-stream", inline = false)))
    }

    @Test
    fun `several files go with one message, by name whatever order they arrive in`() {
        val ann = driver.teammate("ann")
        val files = (1..5).map { FileToSend("file$it.txt", "text/plain", Base64Blob.encode("Number $it")) }

        assertThat(ann.channels.open(general).attachesAll(files.reversed(), "All of them"), equalTo(Verdict.Accepted))

        assertThat(ann.channels.open(general).message(MessageText.of("All of them")).attachments(), equalTo(files.map { Attached(it.name, inline = false) }))
    }

    @Test
    fun `a message with several files and nothing to say goes under the first by name`() {
        val ann = driver.teammate("ann")

        assertThat(ann.channels.open(general).attachesAll(listOf(photo, notes)), equalTo(Verdict.Accepted))

        assertThat(ann.channels.open(general).sees().messages.map { it.text.value }, equalTo(listOf("notes.txt")))
    }

    @Test
    fun `more than ten files at once are refused, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val files = (1..11).map { FileToSend("file$it.txt", "text/plain", Base64Blob.encode("Number $it")) }

        assertThat(ann.channels.open(general).attachesAll(files, "Too many"), equalTo(Verdict.Rejected("Send up to 10 files at once")))

        assertThat(ann.channels.open(general).sees(), equalTo(ChannelSeen(emptyList())))
    }

    @Test
    fun `one bad file refuses them all, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val huge = FileToSend("huge.bin", "application/octet-stream", Base64Blob.encode(ByteArray(5 * 1024 * 1024 + 1)))

        assertThat(ann.channels.open(general).attachesAll(listOf(notes, huge), "Shipped it"), equalTo(Verdict.Rejected("Keep files to 5 MB")))

        assertThat(ann.channels.open(general).sees(), equalTo(ChannelSeen(emptyList())))
    }

    @Test
    fun `a file over five megabytes is refused, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val huge = FileToSend("huge.bin", "application/octet-stream", Base64Blob.encode(ByteArray(5 * 1024 * 1024 + 1)))

        assertThat(ann.channels.open(general).attaches(huge, "Shipped it"), equalTo(Verdict.Rejected("Keep files to 5 MB")))

        assertThat(ann.channels.open(general).sees(), equalTo(ChannelSeen(emptyList())))
    }

    @Test
    fun `only people who can see the channel can send or fetch its files`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        ann.channels.open(secret).attaches(notes, "Plans")

        assertThat(bob.channels.open(secret).downloads("notes.txt")?.contents, equalTo(notes.contents))
        assertThat(cat.channels.open(secret).attaches(photo), equalTo(Verdict.Hidden))
        assertThat(cat.channels.open(secret).downloads("notes.txt"), equalTo(null))

        ann.channels.open(secret).removes(bob)

        eventually { assertThat(bob.channels.open(secret).downloads("notes.txt"), equalTo(null)) }
    }

    @Test
    fun `deleting a message takes its file with it`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).attaches(notes, "Plans")

        ann.channels.open(general).message(plans).deletes()

        assertThat(ann.channels.open(general).downloads("notes.txt"), equalTo(null))
    }
}
