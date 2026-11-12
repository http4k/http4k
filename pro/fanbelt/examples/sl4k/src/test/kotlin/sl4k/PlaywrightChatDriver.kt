package sl4k

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.FilePayload
import com.microsoft.playwright.options.WaitForSelectorState.HIDDEN
import org.http4k.connect.model.Base64Blob
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.MovableClock
import org.http4k.fanbelt.testing.RealBrowser
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.attributeAs
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.navigates
import org.http4k.fanbelt.testing.posting
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.textAs
import org.http4k.fanbelt.testing.verdict
import org.http4k.server.Jetty
import org.http4k.server.uri
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import sl4k.domain.ChannelName
import sl4k.domain.Emoji
import sl4k.domain.Message
import sl4k.domain.MessageText
import sl4k.domain.Viewer
import sl4k.domain.ViewerId
import sl4k.store.Teams
import sl4k.web.viewerCookie
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes

class PlaywrightChatDriver(private val storage: DocumentStorage = InMemoryDocumentStorage()) : ChatDriver, AutoCloseable {
    private val clock = MovableClock()
    private val server = App(AppRuntime(clock = clock, http = fakeSites)) { AppInfra(it, storage) }.asServer(Jetty(0))
    private val browser: RealBrowser
    private val general = ChannelName.of("general")

    init {
        Teams(storage).create(TenantId.of("acme"), clock, general, ChannelName.of("random"))
        server.start()
        browser = RealBrowser(server.uri()).apply { start() }
    }

    override fun teammate(name: String) = PlaywrightTeammate(viewerNamed(name))

    private fun idOf(text: MessageText) = storage.messageNamed(text).id

    override fun tick(by: Duration) = clock.tick(by)

    override fun close() {
        browser.close()
        server.stop()
    }

    inner class PlaywrightTeammate(override val id: ViewerId) : Teammate {
        override val name = Viewer(id).name
        private val page: Page by lazy { browser.blank(viewerCookie(id)) }
        private var on: String? = null
        private var writing = false
        private val windows = mutableMapOf<ChannelName, String>()

        private val messageBox get() = page.locator("form.post input[aria-label=Message]")

        private fun pathTo(channel: ChannelName, message: Message) = if (message.thread == null) pathOf(channel) else "c/${channel.value}/t/${message.thread}"

        private fun said(message: Locator): Said {
            val reactions = message.locator(".reactions .reaction").all()
            return Said(
                message.locator(".author").innerText(),
                message.locator(".text").textAs(MessageText),
                message.locator(".text .mention").all().map { it.getAttribute("data-name")!! }.toSet(),
                reactions = reactions.associate { it.attributeAs<Emoji>("data-emoji") to it.locator(".count").innerText().toInt() },
                mine = reactions.filter { id.toString() in it.getAttribute("data-by")!!.split(" ") }.map { it.attributeAs<Emoji>("data-emoji") }.toSet(),
                edited = message.locator(".edited").count() > 0,
                deleted = message.locator(".text.deleted").count() > 0,
                previews = message.locator(".preview").all().map { it.run { LinkPreview(locator(".title").innerText(), locator(".description").innerText(), locator("img").all().any { it.evaluate("i => i.complete && i.naturalWidth > 0 && i.src.startsWith(location.origin)") == true }) } },
            )
        }

        fun saves(file: String): Pair<String, Base64Blob> {
            val saved = page.waitForDownload { page.getByLabel("Download $file").click() }
            return saved.suggestedFilename() to Base64Blob.encode(saved.path().toFile().readBytes())
        }

        fun drops(vararg files: FileToSend) {
            val drag = dragging(fileIn(*files))
            drag("dragEnter")
            page.locator(".drop").waitFor()
            drag("dragOver")
            drag("drop")
            page.locator("form.post .ready").waitFor()
            page.locator(".drop").waitFor(Locator.WaitForOptions().setState(HIDDEN))
        }

        fun fileReady() = page.locator("form.post .ready").innerText().trim()

        fun fileSent() = page.locator("form.post .ready").waitFor(Locator.WaitForOptions().setState(HIDDEN))

        fun layout() = page.evaluate("getComputedStyle(document.querySelector('main')).display") as String

        fun dragsAway(file: FileToSend) {
            dragging(fileIn(file))("dragEnter")
            page.locator(".drop").waitFor()
            page.locator(".drop input").dispatchEvent("dragleave")
            page.locator(".drop").waitFor(Locator.WaitForOptions().setState(HIDDEN))
        }

        private fun fileIn(vararg files: FileToSend) = JsonObject().apply {
            add("items", JsonArray())
            add("files", JsonArray().apply { files.forEach { file -> add(createTempDirectory().resolve(file.name).apply { writeBytes(file.contents.decodedBytes()) }.toString()) } })
            addProperty("dragOperationsMask", 1)
        }

        private fun dragging(data: JsonObject): (String) -> Unit {
            val devtools = page.context().newCDPSession(page)
            return { type ->
                devtools.send("Input.dispatchDragEvent", JsonObject().apply {
                    addProperty("type", type)
                    addProperty("x", 640)
                    addProperty("y", 300)
                    add("data", data)
                })
            }
        }

        private fun threadPath(channel: ChannelName, of: MessageText) = "c/${channel.value}/t/${idOf(of)}"

        private fun create(channel: String, private: Boolean): Verdict {
            visit(on ?: pathOf(general))
            if (private) page.locator("form.create input.private").check()
            page.locator("form.create input.name").fill(channel)
            val verdict = page.navigates("aside p.error", "/c/$channel") { page.locator("form.create input.name").press("Enter") }
            if (verdict == Verdict.Accepted) on = "c/$channel"
            return verdict
        }

        private fun pathOf(channel: ChannelName) = windows[channel] ?: "c/${channel.value}"

        private fun visit(path: String) {
            if (on == path) return
            val response = page.navigate("/teams/acme/$path")
            if (response?.text().orEmpty().isNotEmpty()) page.locator("body[data-init]").waitFor()
            on = path
        }

        override fun viewing(): ChannelName {
            on = null
            return ChannelName.of(page.url().substringAfter("/c/").substringBefore("/").substringBefore("?"))
        }

        override fun online(): Set<String> {
            visit(on ?: pathOf(general))
            return page.locator("#people li.here .name").allInnerTexts().toSet()
        }

        override fun closes() {
            page.navigate("about:blank")
            on = null
        }

        override fun you(): String {
            visit(on ?: pathOf(general))
            return page.locator("#people li.you .name").innerText()
        }

        override val channels: Channels = object : Channels {
            override fun searches(query: String): List<Found> {
                val path = "search?q=$query"
                if (on != path) {
                    visit(on ?: pathOf(general))
                    page.locator("form.search input").fill(query)
                    page.locator("form.search input").press("Enter")
                    page.waitForURL("**/search?q=*")
                    page.locator("#results").waitFor()
                    on = path
                }
                return page.locator("#results .result").all().map {
                    Found(it.locator(".channel").innerText(), it.locator(".author").innerText(), it.locator(".text").textAs(MessageText))
                }
            }

            override fun open(channel: ChannelName): OpenChannel {
                visit(pathOf(channel))
                return channelOn(channel)
            }

            override fun creates(channel: String) = create(channel, private = false)

            override fun createsPrivate(channel: ChannelName) = create(channel.value, private = true)

            override fun list(): List<ChannelName> {
                visit(on ?: pathOf(general))
                return page.locator("#channels .channels a").all().map { it.attributeAs("data-channel", ChannelName) }
            }

            override fun unread(): Map<ChannelName, Int> {
                visit(on ?: pathOf(general))
                return page.locator("#channels a:has(.unread)").all().associate { it.attributeAs("data-channel", ChannelName) to it.locator(".unread").innerText().toInt() }
            }

            override fun mentions(): Map<ChannelName, Int> {
                visit(on ?: pathOf(general))
                return page.locator("#channels a:has(.mentioned)").all().associate { it.attributeAs("data-channel", ChannelName) to it.locator(".mentioned").innerText().removePrefix("@").toInt() }
            }

            override fun messagesDirectly(who: Teammate): ChannelName {
                visit(on ?: pathOf(general))
                val from = page.url()
                page.locator("#people button[data-viewer='${who.id}']").click()
                page.waitForURL { it != from && it.contains("/c/dm-") }
                val channel = page.url().substringAfter("/c/")
                on = "c/$channel"
                return ChannelName.of(channel)
            }

            override fun directMessages(): Map<String, Int> {
                visit(on ?: pathOf(general))
                return page.locator("#channels .direct a").all().associate { link ->
                    link.getAttribute("data-with")!! to (link.locator(".unread").allInnerTexts().firstOrNull()?.toInt() ?: 0)
                }
            }
        }

        private fun channelOn(channel: ChannelName): OpenChannel = object : OpenChannel {
            override val topic = topicOn(channel)
            override val bookmarks = bookmarksOn(channel)
            override val canvas = canvasOn(channel)

            override fun message(text: MessageText) = messageOn(channel, text)

            override fun attachesAll(files: List<FileToSend>, text: String): Verdict {
                visit(pathOf(channel))
                val picker = page.locator("form.post input[type=file]")
                if (picker.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/messages")
                val leaked = CopyOnWriteArrayList<String>()
                val watch = Consumer<com.microsoft.playwright.Request> { request -> if (!request.url().endsWith("/messages")) request.postData()?.let(leaked::add) }
                page.onRequest(watch)
                picker.setInputFiles(files.map { FilePayload(it.name, it.mime, it.contents.decodedBytes()) }.toTypedArray())
                page.locator("form.post .ready").waitFor()
                messageBox.pressSequentially("x")
                messageBox.fill(text)
                val verdict = page.verdict("main p.error") { messageBox.press("Enter") }
                page.offRequest(watch)
                check(leaked.none { sent -> files.any { it.contents.value in sent } }) { "the file went out on something other than the message" }
                return verdict
            }

            override fun downloads(file: String): Downloaded? {
                val found = storage.attachmentNamed(file) ?: return null
                val response = page.request().get("/teams/acme/c/${channel.value}/files/${found.id}")
                if (!response.ok()) return null
                return Downloaded(Base64Blob.encode(response.body()), response.headers().getValue("content-type"), response.headers().getValue("content-disposition").startsWith("inline"))
            }

            override fun posts(text: String): Verdict {
                visit(pathOf(channel))
                if (messageBox.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/messages")
                messageBox.fill(text)
                return page.verdict("main p.error") { messageBox.press("Enter") }
            }

            override fun sees(): ChannelSeen {
                visit(pathOf(channel))
                return ChannelSeen(
                    page.locator("#history .message").all().map { message ->
                        said(message).copy(replies = message.locator("a.replies").allInnerTexts().firstOrNull()?.substringBefore(" ")?.toInt() ?: 0)
                    }.reversed(),
                    page.locator("#history a.older").count() > 0,
                )
            }

            override fun firstUnread(): String? {
                visit(pathOf(channel))
                return page.evaluate("[...document.querySelectorAll('#history .message')].find(m => getComputedStyle(m, '::before').content !== 'none')?.querySelector('.text').innerText ?? null") as String?
            }

            override fun showsOlder() {
                visit(pathOf(channel))
                windows[channel] = page.locator("#history a.older").getAttribute("href")!!
            }

            override fun canRead(): Boolean {
                visit(pathOf(channel))
                return page.locator("#history").count() > 0
            }

            override fun heading(): String {
                visit(pathOf(channel))
                return page.locator("main header .heading").innerText()
            }

            override fun invites(who: Teammate): Verdict {
                visit(pathOf(channel))
                val button = page.locator("#members button.invite[data-viewer='${who.id}']")
                if (button.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/invite/${who.id}")
                return page.verdict("main p.error") { button.click() }
            }

            override fun removes(who: Teammate): Verdict {
                visit(pathOf(channel))
                val button = page.locator("#members button.remove[data-viewer='${who.id}']")
                if (button.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/remove/${who.id}")
                return page.verdict("p.error") { button.click() }
            }

            override fun leaves(): Verdict {
                visit(pathOf(channel))
                if (page.locator("#members button.leave").count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/leave")
                val verdict = page.navigates("p.error", "/c/general") { page.locator("#members button.leave").click() }
                if (verdict == Verdict.Accepted) on = "c/general"
                return verdict
            }

            override fun types(text: String) {
                visit(pathOf(channel))
                messageBox.pressSequentially(text)
            }

            override fun typing(): Set<String> {
                visit(pathOf(channel))
                return page.locator("#typing .who").allInnerTexts().toSet()
            }

            override fun suggestions(typed: String): Set<String> {
                visit(pathOf(channel))
                messageBox.fill("")
                page.waitForResponse({ it.url().endsWith("/suggest") }) { messageBox.pressSequentially(typed) }.finished()
                page.evaluate("() => new Promise(done => requestAnimationFrame(() => done()))")
                return page.locator("#suggest .suggestion").all().map { it.innerText().substringAfter(" ") }.toSet()
            }

            override fun tabs(typed: String): String {
                visit(pathOf(channel))
                messageBox.fill("")
                messageBox.pressSequentially(typed)
                page.waitForResponse({ it.url().endsWith("/complete") }) { messageBox.press("Tab") }
                page.waitForFunction("() => document.querySelector('form.post input').value.endsWith(' ')")
                assertEquals(true, page.evaluate("() => document.activeElement === document.querySelector('form.post input[aria-label=Message]')"))
                return messageBox.inputValue()
            }

            override fun pinned(): List<Pinned> {
                visit(pathOf(channel))
                return page.locator("#pins .pin").all().map { Pinned(it.locator(".author").innerText(), it.locator(".said").innerText()) }
            }
        }

        private fun topicOn(channel: ChannelName): TopicLine = object : TopicLine {
            override fun sets(text: String): Verdict {
                visit(pathOf(channel))
                val button = page.locator("#topic button.edit-topic")
                if (button.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/topic")
                if (page.locator("#topic .cancel-topic").isVisible()) page.locator("#topic .cancel-topic").click()
                button.click()
                page.locator("#topic form input").waitFor()
                page.keyboard().press("ControlOrMeta+A")
                page.keyboard().type(text)
                return page.verdict("main p.error") { page.keyboard().press("Enter") }
            }

            override fun read(): String {
                visit(pathOf(channel))
                return page.locator("#topic .text").allInnerTexts().firstOrNull().orEmpty()
            }
        }

        private fun bookmarksOn(channel: ChannelName): BookmarkBar = object : BookmarkBar {
            override fun adds(title: String, url: String): Verdict {
                visit(pathOf(channel))
                val button = page.locator("#bookmarks button.add-bookmark")
                if (button.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/bookmarks")
                if (page.locator("#bookmarks .cancel-bookmark").isVisible()) page.locator("#bookmarks .cancel-bookmark").click()
                button.click()
                page.locator("#bookmarks form input.title").fill(title)
                page.locator("#bookmarks form input.url").fill(url)
                return page.verdict("main p.error") { page.locator("#bookmarks form input.url").press("Enter") }
            }

            override fun removes(title: String): Verdict {
                visit(pathOf(channel))
                val bookmark = storage.bookmarkNamed(title)
                val button = page.locator("#b-${bookmark.id} .remove-bookmark")
                if (button.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/bookmarks/${bookmark.id}/remove")
                return page.verdict("main p.error") { button.click() }
            }

            override fun list(): List<Bookmarked> {
                visit(pathOf(channel))
                return page.locator("#bookmarks .bookmark a").all().map { Bookmarked(it.innerText(), it.getAttribute("href")!!) }
            }
        }

        private fun canvasOn(channel: ChannelName): CanvasPanel = object : CanvasPanel {
            override fun begins() {
                visit(pathOf(channel))
                if (writing) page.locator("#canvas .cancel-canvas").click()
                if (!page.locator("#canvas").isVisible()) page.locator(".toggle-canvas").click()
                page.locator("#canvas .edit-canvas").click()
                page.locator("#canvas textarea").waitFor()
                writing = true
            }

            override fun saves(text: String): Verdict {
                visit(pathOf(channel))
                if (page.locator("#canvas").count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/canvas")
                if (!writing) begins()
                page.locator("#canvas textarea").fill(text)
                val verdict = page.verdict("main p.error") { page.locator("#canvas .save-canvas").click() }
                writing = verdict != Verdict.Accepted
                return verdict
            }

            override fun read(): String {
                visit(pathOf(channel))
                return page.locator("#canvas .body").allTextContents().firstOrNull().orEmpty()
            }
        }

        private fun messageOn(channel: ChannelName, said: MessageText): PostedMessage = object : PostedMessage {
            override fun attachments(): List<Attached> {
                visit(pathOf(channel))
                return page.locator("#m-${idOf(said)} .attachment").all().map { Attached(it.getAttribute("data-name")!!, it.locator("img").count() > 0) }
            }

            override fun opensThread() = visit(threadPath(channel, said))

            override fun thread(): ThreadSeen {
                visit(threadPath(channel, said))
                val replies = page.locator("#thread .reply").all().map(::said)
                return ThreadSeen(said(page.locator("#thread .parent")).copy(replies = replies.size), replies)
            }

            override fun replies(text: String): Verdict {
                visit(threadPath(channel, said))
                if (page.locator("form.reply input").count() == 0) return page.provesServerRefuses("/teams/acme/${threadPath(channel, said)}/replies")
                page.locator("form.reply input").fill(text)
                return page.verdict("p.error") { page.locator("form.reply input").press("Enter") }
            }

            override fun reacts(emoji: Emoji): Verdict {
                val message = storage.messageNamed(said)
                visit(pathTo(channel, message))
                val button = page.locator("#m-${message.id} .add-reaction[data-emoji='$emoji'], #r-${message.id} .add-reaction[data-emoji='$emoji']")
                if (button.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/m/${message.id}/react/$emoji")
                return page.verdict("p.error") { button.first().dispatchEvent("click") }
            }

            override fun edits(to: String): Verdict {
                val found = storage.messageNamed(said)
                if (found.author != id) return page.provesServerRefuses("/teams/acme/c/${channel.value}/m/${found.id}/edit", """{"edit":"$to"}""")
                visit(pathTo(channel, found))
                page.locator("#m-${found.id}, #r-${found.id}").first().hover()
                page.posting { page.locator("#m-${found.id} .edit-message, #r-${found.id} .edit-message").click() }
                val box = page.locator("#m-${found.id} form.edit input, #r-${found.id} form.edit input")
                box.fill(to)
                return page.verdict("p.error") { box.press("Enter") }
            }

            override fun deletes(): Verdict {
                val found = storage.messageNamed(said)
                if (found.author != id) return page.provesServerRefuses("/teams/acme/c/${channel.value}/m/${found.id}/delete")
                visit(pathTo(channel, found))
                page.locator("#m-${found.id}, #r-${found.id}").first().hover()
                return page.verdict("p.error") { page.locator("#m-${found.id} .delete-message, #r-${found.id} .delete-message").click() }
            }

            override fun togglesPin(): Verdict {
                visit(pathOf(channel))
                val found = idOf(said)
                val button = page.locator("#m-$found .toggle-pin")
                if (button.count() == 0) return page.provesServerRefuses("/teams/acme/c/${channel.value}/m/$found/pin")
                page.locator("#m-$found").hover()
                return page.verdict("main p.error") { button.click() }
            }

            override fun unpinsFromTheTop(): Verdict {
                visit(pathOf(channel))
                return page.verdict("main p.error") { page.locator("#p-${idOf(said)} .unpin").click() }
            }
        }
    }
}

@EnabledIfEnvironmentVariable(named = "FANBELT_TEST_BROWSER", matches = ".+")
class ChatPlaywrightTest : ChatContract() {
    override val driver = PlaywrightChatDriver()

    @Test
    fun `a file dropped anywhere on the page is attached to the next message`() {
        val ann = driver.teammate("ann")
        val general = ChannelName.of("general")
        val photo = FileToSend("photo.png", "image/png", Base64Blob.encode(byteArrayOf(-119, 80, 78, 71)))
        ann.channels.open(general)

        ann.drops(photo)
        ann.channels.open(general).posts("Look at this")

        eventually { assertEquals(listOf(Attached("photo.png", inline = true)), ann.channels.open(general).message(MessageText.of("Look at this")).attachments()) }
    }

    @Test
    fun `a dropped file shows its name until it has been sent`() {
        val ann = driver.teammate("ann")
        val general = ChannelName.of("general")
        ann.channels.open(general)

        ann.drops(FileToSend("photo.png", "image/png", Base64Blob.encode(byteArrayOf(-119, 80, 78, 71))))

        assertEquals("\uD83D\uDCC4 photo.png", ann.fileReady())

        ann.channels.open(general).posts("Look at this")

        ann.fileSent()
    }

    @Test
    fun `several files dropped together all go with the message`() {
        val ann = driver.teammate("ann")
        val general = ChannelName.of("general")
        val notes = FileToSend("notes.txt", "text/plain", Base64Blob.encode("Remember the milk"))
        val photo = FileToSend("photo.png", "image/png", Base64Blob.encode(byteArrayOf(-119, 80, 78, 71)))
        ann.channels.open(general)

        ann.drops(notes, photo)

        assertEquals(setOf("notes.txt", "photo.png"), ann.fileReady().removePrefix("\uD83D\uDCC4 ").split(", ").toSet())

        ann.channels.open(general).posts("Both")

        eventually { assertEquals(listOf(Attached("notes.txt", inline = false), Attached("photo.png", inline = true)), ann.channels.open(general).message(MessageText.of("Both")).attachments()) }
    }

    @Test
    fun `the download button saves the file, even one that would open in the browser`() {
        val ann = driver.teammate("ann")
        val general = ChannelName.of("general")
        val notes = FileToSend("notes.txt", "text/plain", Base64Blob.encode("Remember the milk"))
        val photo = FileToSend("photo.png", "image/png", Base64Blob.encode(byteArrayOf(-119, 80, 78, 71)))
        ann.channels.open(general)
        ann.drops(notes, photo)
        ann.channels.open(general).posts("Both")

        assertEquals("notes.txt" to notes.contents, ann.saves("notes.txt"))
        assertEquals("photo.png" to photo.contents, ann.saves("photo.png"))
    }

    @Test
    fun `the same file can be dropped again after it has been sent`() {
        val ann = driver.teammate("ann")
        val general = ChannelName.of("general")
        val photo = FileToSend("photo.png", "image/png", Base64Blob.encode(byteArrayOf(-119, 80, 78, 71)))
        ann.channels.open(general)
        ann.drops(photo)
        ann.channels.open(general).posts("First")
        ann.fileSent()

        ann.drops(photo)

        assertEquals("\uD83D\uDCC4 photo.png", ann.fileReady())
    }

    @Test
    fun `dragging a file away puts the page back`() {
        val ann = driver.teammate("ann")
        val general = ChannelName.of("general")
        ann.channels.open(general)

        ann.dragsAway(FileToSend("photo.png", "image/png", Base64Blob.encode(byteArrayOf(-119, 80, 78, 71))))

        assertEquals(Verdict.Accepted, ann.channels.open(general).posts("Still here"))
    }

    @Test
    fun `the page is laid out by its stylesheet`() {
        val ann = driver.teammate("ann")
        ann.channels.open(ChannelName.of("general"))

        assertEquals("grid", ann.layout())
    }

    @AfterEach
    fun close() = driver.close()
}