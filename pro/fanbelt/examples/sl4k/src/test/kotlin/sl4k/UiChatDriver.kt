package sl4k

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.connect.model.Base64Blob
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.testing.patched.toHttpHandler
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.attributeAs
import org.http4k.fanbelt.testing.browser
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.textAs
import org.http4k.fanbelt.testing.verdict
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import sl4k.domain.ChannelName
import sl4k.domain.Emoji
import sl4k.domain.Message
import sl4k.domain.MessageText
import sl4k.domain.Viewer
import sl4k.domain.ViewerId
import sl4k.store.Teams
import sl4k.web.viewerCookie
import java.time.Duration

class UiChatDriver(private val storage: DocumentStorage = InMemoryDocumentStorage()) : ChatDriver {
    private val scheduler = DeterministicScheduler(start)
    private val node = App(TestRuntime(scheduler, http = fakeSites)) { AppInfra(it, storage) }
    private val general = ChannelName.of("general")

    init {
        Teams(storage).create(TenantId.of("acme"), TestRuntime(scheduler).clock, general, ChannelName.of("random"))
        node.start()
    }

    override fun teammate(name: String): Teammate = UiTeammate(viewerNamed(name))

    private fun idOf(text: MessageText) = storage.messageNamed(text).id

    override fun tick(by: Duration) {
        scheduler.tick(by)
        scheduler.runUntilIdle()
    }

    private inner class UiTeammate(override val id: ViewerId) : Teammate {
        override val name = Viewer(id).name
        private val browser = browser(node, scheduler, viewerCookie(id))
        private var on: String? = null
        private var writing = false
        private val windows = mutableMapOf<ChannelName, String>()

        private fun pathTo(channel: ChannelName, message: Message) = if (message.thread == null) pathOf(channel) else "c/${channel.value}/t/${message.thread}"

        private fun said(message: WebElement): Said {
            val reactions = message.findElements(By.cssSelector(".reactions .reaction"))
            return Said(
                message.findElement(By.className("author")).text,
                message.findElement(By.className("text")).textAs(MessageText),
                message.findElements(By.cssSelector(".text .mention")).map { it.getAttribute("data-name")!! }.toSet(),
                reactions = reactions.associate { it.attributeAs<Emoji>("data-emoji") to it.findElement(By.className("count")).text.toInt() },
                mine = reactions.filter { id.toString() in it.getAttribute("data-by")!!.split(" ") }.map { it.attributeAs<Emoji>("data-emoji") }.toSet(),
                edited = message.findElements(By.className("edited")).isNotEmpty(),
                deleted = message.findElements(By.cssSelector(".text.deleted")).isNotEmpty(),
                previews = message.findElements(By.className("preview")).map { it.run { LinkPreview(findElement(By.className("title")).text, findElement(By.className("description")).text, findElements(By.cssSelector("img[src^='c/']")).isNotEmpty()) } },
            )
        }

        private fun threadPath(channel: ChannelName, of: MessageText) = "c/${channel.value}/t/${idOf(of)}"

        private fun create(channel: String, private: Boolean): Verdict {
            visit(on ?: pathOf(general))
            if (private) browser.findElement(By.cssSelector("form.create input.private")).click()
            browser.findElement(By.cssSelector("form.create input.name")).run {
                clear()
                sendKeys(channel)
                submit()
            }
            val verdict = browser.verdict(By.cssSelector("p.error"))
            if (verdict == Verdict.Accepted) on = null
            return verdict
        }

        private fun pathOf(channel: ChannelName) = windows[channel] ?: "c/${channel.value}"

        private fun visit(path: String) {
            if (on == path) return
            browser.get("http://localhost/teams/acme/$path")
            on = path
        }

        override fun viewing(): ChannelName {
            on = null
            browser.settle()
            return ChannelName.of(browser.currentUrl!!.substringAfter("/c/").substringBefore("/").substringBefore("?"))
        }

        override fun online(): Set<String> {
            visit(on ?: pathOf(general))
            return browser.findElements(By.cssSelector("#people li.here .name")).map { it.text }.toSet()
        }

        override fun closes() {
            browser.close()
            tick(Duration.ofMinutes(1))
            on = null
        }

        override fun you(): String {
            visit(on ?: pathOf(general))
            return browser.findElement(By.cssSelector("#people li.you .name")).text
        }

        override val channels: Channels = object : Channels {
            override fun searches(query: String): List<Found> {
                val path = "search?q=$query"
                if (on != path) {
                    visit(on ?: pathOf(general))
                    browser.findElement(By.cssSelector("form.search input")).run {
                        clear()
                        sendKeys(query)
                        submit()
                    }
                    on = path
                }
                return browser.findElements(By.cssSelector("#results .result")).map {
                    Found(it.findElement(By.className("channel")).text, it.findElement(By.className("author")).text, it.findElement(By.className("text")).textAs(MessageText))
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
                return browser.findElements(By.cssSelector("#channels .channels a")).map { it.attributeAs("data-channel", ChannelName) }
            }

            override fun unread(): Map<ChannelName, Int> {
                visit(on ?: pathOf(general))
                return browser.findElements(By.cssSelector("#channels a")).mapNotNull { link ->
                    link.findElements(By.className("unread")).firstOrNull()?.let { link.attributeAs("data-channel", ChannelName) to it.text.toInt() }
                }.toMap()
            }

            override fun mentions(): Map<ChannelName, Int> {
                visit(on ?: pathOf(general))
                return browser.findElements(By.cssSelector("#channels a")).mapNotNull { link ->
                    link.findElements(By.className("mentioned")).firstOrNull()?.let { link.attributeAs("data-channel", ChannelName) to it.text.removePrefix("@").toInt() }
                }.toMap()
            }

            override fun messagesDirectly(who: Teammate): ChannelName {
                visit(on ?: pathOf(general))
                browser.findElement(By.cssSelector("#people button[data-viewer='${who.id}']")).click()
                val channel = browser.currentUrl!!.substringAfter("/c/")
                on = "c/$channel"
                return ChannelName.of(channel)
            }

            override fun directMessages(): Map<String, Int> {
                visit(on ?: pathOf(general))
                return browser.findElements(By.cssSelector("#channels .direct a")).associate { link ->
                    link.getAttribute("data-with")!! to (link.findElements(By.className("unread")).firstOrNull()?.text?.toInt() ?: 0)
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
                val upload = files.joinToString(",", "[", "]") { """{"name":"${it.name}","mime":"${it.mime}","contents":"${it.contents}"}""" }
                val response = node.handler.toHttpHandler()(datastarPost("c/${channel.value}/messages", viewerCookie(id), """{"text":"$text","upload":$upload}"""))
                tick(Duration.ZERO)
                browser.settle()
                if (response.status == NOT_FOUND) return Verdict.Hidden
                return Regex(""""upload":"([^"]+)"""").find(response.bodyString())?.let { Verdict.Rejected(it.groupValues[1]) } ?: Verdict.Accepted
            }

            override fun downloads(file: String): Downloaded? {
                val found = storage.attachmentNamed(file) ?: return null
                val response = node.handler.toHttpHandler()(Request(GET, "/teams/acme/c/${channel.value}/files/${found.id}").cookie(viewerCookie(id)))
                if (response.status != OK) return null
                return Downloaded(Base64Blob.encode(response.body.payload.array()), response.header("content-type")!!, response.header("content-disposition")!!.startsWith("inline"))
            }

            override fun posts(text: String): Verdict {
                visit(pathOf(channel))
                val box = browser.findElements(By.cssSelector("form.post input[aria-label=Message]")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/messages", viewerCookie(id))
                box.run {
                    clear()
                    sendKeys(text)
                    submit()
                }
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun sees(): ChannelSeen {
                visit(pathOf(channel))
                return ChannelSeen(
                    browser.findElements(By.cssSelector("#history .message")).map {
                        said(it).copy(replies = it.findElements(By.cssSelector("a.replies")).firstOrNull()?.text?.substringBefore(" ")?.toInt() ?: 0)
                    }.reversed(),
                    browser.findElements(By.cssSelector("#history a.older")).isNotEmpty(),
                )
            }

            override fun firstUnread(): String? {
                visit(pathOf(channel))
                val id = Regex("#m-([0-9a-f-]+)::before").find(browser.pageSource.orEmpty())?.groupValues?.get(1)
                return browser.findElements(By.cssSelector("#m-$id .text")).firstOrNull()?.text
            }

            override fun showsOlder() {
                visit(pathOf(channel))
                windows[channel] = browser.findElement(By.cssSelector("#history a.older")).getAttribute("href")!!
            }

            override fun canRead(): Boolean {
                visit(pathOf(channel))
                return browser.findElements(By.cssSelector("#history")).isNotEmpty()
            }

            override fun heading(): String {
                visit(pathOf(channel))
                return browser.findElement(By.cssSelector("main header .heading")).text
            }

            override fun invites(who: Teammate): Verdict {
                visit(pathOf(channel))
                val button = browser.findElements(By.cssSelector("#members button.invite[data-viewer='${who.id}']")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/invite/${who.id}", viewerCookie(id))
                button.click()
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun removes(who: Teammate): Verdict {
                visit(pathOf(channel))
                val button = browser.findElements(By.cssSelector("#members button.remove[data-viewer='${who.id}']")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/remove/${who.id}", viewerCookie(id))
                button.click()
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun leaves(): Verdict {
                visit(pathOf(channel))
                val button = browser.findElements(By.cssSelector("#members button.leave")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/leave", viewerCookie(id))
                button.click()
                val verdict = browser.verdict(By.cssSelector("p.error"))
                if (verdict == Verdict.Accepted) on = null
                return verdict
            }

            override fun types(text: String) {
                visit(pathOf(channel))
                browser.findElement(By.cssSelector("form.post input[aria-label=Message]")).sendKeys(text)
            }

            override fun typing(): Set<String> {
                visit(pathOf(channel))
                return browser.findElements(By.cssSelector("#typing .who")).map { it.text }.toSet()
            }

            override fun suggestions(typed: String): Set<String> {
                visit(pathOf(channel))
                browser.findElement(By.cssSelector("form.post input[aria-label=Message]")).run {
                    clear()
                    sendKeys(typed)
                }
                tick(Duration.ofMillis(200))
                return browser.findElements(By.cssSelector("#suggest .suggestion")).map { it.text.substringAfter(" ") }.toSet()
            }

            override fun tabs(typed: String): String {
                visit(pathOf(channel))
                val box = browser.findElement(By.cssSelector("form.post input[aria-label=Message]"))
                box.clear()
                box.sendKeys(typed + Keys.TAB)
                return box.getAttribute("value")!!
            }

            override fun pinned(): List<Pinned> {
                visit(pathOf(channel))
                return browser.findElements(By.cssSelector("#pins .pin")).map { Pinned(it.findElement(By.className("author")).text, it.findElement(By.className("said")).text) }
            }
        }

        private fun topicOn(channel: ChannelName): TopicLine = object : TopicLine {
            override fun sets(text: String): Verdict {
                visit(pathOf(channel))
                val button = browser.findElements(By.cssSelector("#topic button.edit-topic")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/topic", viewerCookie(id), """{"topic":"$text"}""")
                button.click()
                browser.findElement(By.cssSelector("#topic form input")).run {
                    clear()
                    sendKeys(text)
                    submit()
                }
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun read(): String {
                visit(pathOf(channel))
                return browser.findElements(By.cssSelector("#topic .text")).firstOrNull()?.text.orEmpty()
            }
        }

        private fun bookmarksOn(channel: ChannelName): BookmarkBar = object : BookmarkBar {
            override fun adds(title: String, url: String): Verdict {
                visit(pathOf(channel))
                val button = browser.findElements(By.cssSelector("#bookmarks button.add-bookmark")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/bookmarks", viewerCookie(id))
                button.click()
                browser.findElement(By.cssSelector("#bookmarks form input.title")).run {
                    clear()
                    sendKeys(title)
                }
                browser.findElement(By.cssSelector("#bookmarks form input.url")).run {
                    clear()
                    sendKeys(url)
                    submit()
                }
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun removes(title: String): Verdict {
                visit(pathOf(channel))
                val bookmark = storage.bookmarkNamed(title)
                val button = browser.findElements(By.cssSelector("#b-${bookmark.id} .remove-bookmark")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/bookmarks/${bookmark.id}/remove", viewerCookie(id))
                button.click()
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun list(): List<Bookmarked> {
                visit(pathOf(channel))
                return browser.findElements(By.cssSelector("#bookmarks .bookmark a")).map { Bookmarked(it.text, it.getAttribute("href")!!) }
            }
        }

        private fun canvasOn(channel: ChannelName): CanvasPanel = object : CanvasPanel {
            override fun begins() {
                visit(pathOf(channel))
                if (writing) browser.findElement(By.cssSelector("#canvas .cancel-canvas")).click()
                browser.findElements(By.cssSelector("#canvas .edit-canvas")).firstOrNull()?.click()
                writing = true
            }

            override fun saves(text: String): Verdict {
                visit(pathOf(channel))
                if (browser.findElements(By.cssSelector("#canvas")).isEmpty()) return node.provesServerRefuses("/teams/acme/c/${channel.value}/canvas", viewerCookie(id))
                if (!writing) begins()
                browser.findElement(By.cssSelector("#canvas textarea")).run {
                    clear()
                    sendKeys(text)
                }
                browser.findElement(By.cssSelector("#canvas .save-canvas")).click()
                val verdict = browser.verdict(By.cssSelector("p.error"))
                writing = verdict != Verdict.Accepted
                return verdict
            }

            override fun read(): String {
                visit(pathOf(channel))
                return Jsoup.parse(browser.pageSource).selectFirst("#canvas .body")?.wholeText().orEmpty()
            }
        }

        private fun messageOn(channel: ChannelName, said: MessageText): PostedMessage = object : PostedMessage {
            override fun attachments(): List<Attached> {
                visit(pathOf(channel))
                return browser.findElements(By.cssSelector("#m-${idOf(said)} .attachment")).map { Attached(it.getAttribute("data-name")!!, it.findElements(By.tagName("img")).isNotEmpty()) }
            }

            override fun opensThread() = visit(threadPath(channel, said))

            override fun thread(): ThreadSeen {
                visit(threadPath(channel, said))
                val replies = browser.findElements(By.cssSelector("#thread .reply")).map(::said)
                return ThreadSeen(said(browser.findElement(By.cssSelector("#thread .parent"))).copy(replies = replies.size), replies)
            }

            override fun replies(text: String): Verdict {
                visit(threadPath(channel, said))
                val box = browser.findElements(By.cssSelector("form.reply input")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/${threadPath(channel, said)}/replies", viewerCookie(id))
                box.run {
                    clear()
                    sendKeys(text)
                    submit()
                }
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun reacts(emoji: Emoji): Verdict {
                val message = storage.messageNamed(said)
                visit(pathTo(channel, message))
                val button = browser.findElements(By.cssSelector("#m-${message.id} .add-reaction[data-emoji='$emoji'], #r-${message.id} .add-reaction[data-emoji='$emoji']")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/m/${message.id}/react/$emoji", viewerCookie(id))
                button.click()
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun edits(to: String): Verdict {
                val found = storage.messageNamed(said)
                if (found.author != id) return node.provesServerRefuses("/teams/acme/c/${channel.value}/m/${found.id}/edit", viewerCookie(id), """{"edit":"$to"}""")
                visit(pathTo(channel, found))
                browser.findElement(By.cssSelector("#m-${found.id} .edit-message, #r-${found.id} .edit-message")).click()
                browser.findElement(By.cssSelector("#m-${found.id} form.edit input, #r-${found.id} form.edit input")).run {
                    clear()
                    sendKeys(to)
                    submit()
                }
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun deletes(): Verdict {
                val found = storage.messageNamed(said)
                if (found.author != id) return node.provesServerRefuses("/teams/acme/c/${channel.value}/m/${found.id}/delete", viewerCookie(id))
                visit(pathTo(channel, found))
                browser.findElement(By.cssSelector("#m-${found.id} .delete-message, #r-${found.id} .delete-message")).click()
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun togglesPin(): Verdict {
                visit(pathOf(channel))
                val found = idOf(said)
                val button = browser.findElements(By.cssSelector("#m-$found .toggle-pin")).firstOrNull()
                    ?: return node.provesServerRefuses("/teams/acme/c/${channel.value}/m/$found/pin", viewerCookie(id))
                button.click()
                return browser.verdict(By.cssSelector("p.error"))
            }

            override fun unpinsFromTheTop(): Verdict {
                visit(pathOf(channel))
                browser.findElement(By.cssSelector("#p-${idOf(said)} .unpin")).click()
                return browser.verdict(By.cssSelector("p.error"))
            }
        }
    }
}