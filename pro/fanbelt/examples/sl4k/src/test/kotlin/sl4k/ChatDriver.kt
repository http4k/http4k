package sl4k

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.MOVED_PERMANENTLY
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.connect.model.Base64Blob
import org.http4k.fanbelt.testing.Verdict
import sl4k.domain.ChannelName
import sl4k.domain.Emoji
import sl4k.domain.MessageText
import sl4k.domain.Viewer
import sl4k.domain.ViewerId
import java.time.Duration
import java.nio.ByteBuffer
import java.util.Base64

interface ChatDriver {
    fun teammate(name: String): Teammate
    fun tick(by: Duration)
}

interface Teammate {
    val id: ViewerId
    val name: String
    val handle: String get() = Viewer(id).handle
    val channels: Channels
    fun viewing(): ChannelName
    fun online(): Set<String>
    fun you(): String
    fun closes()
}

interface Channels {
    fun open(channel: ChannelName): OpenChannel
    fun creates(channel: String): Verdict
    fun createsPrivate(channel: ChannelName): Verdict
    fun list(): List<ChannelName>
    fun unread(): Map<ChannelName, Int>
    fun mentions(): Map<ChannelName, Int>
    fun messagesDirectly(who: Teammate): ChannelName
    fun directMessages(): Map<String, Int>
    fun searches(query: String): List<Found>
}

interface OpenChannel {
    val topic: TopicLine
    val bookmarks: BookmarkBar
    val canvas: CanvasPanel
    fun message(text: MessageText): PostedMessage
    fun posts(text: String): Verdict
    fun attaches(file: FileToSend, text: String = "") = attachesAll(listOf(file), text)
    fun attachesAll(files: List<FileToSend>, text: String = ""): Verdict
    fun downloads(file: String): Downloaded?
    fun sees(): ChannelSeen
    fun firstUnread(): String?
    fun showsOlder()
    fun canRead(): Boolean
    fun heading(): String
    fun invites(who: Teammate): Verdict
    fun removes(who: Teammate): Verdict
    fun leaves(): Verdict
    fun types(text: String)
    fun typing(): Set<String>
    fun suggestions(typed: String): Set<String>
    fun tabs(typed: String): String
    fun pinned(): List<Pinned>
}

interface PostedMessage {
    fun attachments(): List<Attached>
    fun opensThread()
    fun thread(): ThreadSeen
    fun replies(text: String): Verdict
    fun reacts(emoji: Emoji): Verdict
    fun edits(to: String): Verdict
    fun deletes(): Verdict
    fun togglesPin(): Verdict
    fun unpinsFromTheTop(): Verdict
}

interface TopicLine {
    fun sets(text: String): Verdict
    fun read(): String
}

interface BookmarkBar {
    fun adds(title: String, url: String): Verdict
    fun removes(title: String): Verdict
    fun list(): List<Bookmarked>
}

interface CanvasPanel {
    fun begins()
    fun saves(text: String): Verdict
    fun read(): String
}

data class LinkPreview(val title: String, val description: String, val image: Boolean = false)

val tinyPng: ByteArray = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")

val fakeSites: HttpHandler = { request ->
    when (request.uri.path) {
        "/article" -> Response(OK).body("""<html><head><title>Ignored</title><meta property="og:title" content="An article"><meta property="og:description" content="All about it"></head></html>""")
        "/plain" -> Response(OK).body("""<html><head><title>A plain page</title><meta name="description" content="Nothing fancy"></head></html>""")
        "/moved" -> Response(MOVED_PERMANENTLY).header("location", "https://example.test/article")
        "/pictured" -> Response(OK).body("""<html><head><meta property="og:title" content="Pictured"><meta property="og:image" content="/hero.png"></head></html>""")
        "/vector" -> Response(OK).body("""<html><head><meta property="og:title" content="Vector"><meta property="og:image" content="https://example.test/hero.svg"></head></html>""")
        "/broken" -> Response(OK).body("""<html><head><meta property="og:title" content="Broken"><meta property="og:image" content="https://example.test/missing.png"></head></html>""")
        "/hero.png" -> Response(OK).header("content-type", "image/png").body(Body(ByteBuffer.wrap(tinyPng)))
        "/hero.svg" -> Response(OK).header("content-type", "image/svg+xml").body("<svg onload='alert(1)'/>")
        "/untitled" -> Response(OK).body("<p>No head at all</p>")
        else -> Response(NOT_FOUND)
    }
}

data class Found(val channel: String, val author: String, val text: MessageText)

data class ChannelSeen(val messages: List<Said>, val older: Boolean = false)

data class Said(
    val author: String,
    val text: MessageText,
    val mentions: Set<String> = emptySet(),
    val replies: Int = 0,
    val reactions: Map<Emoji, Int> = emptyMap(),
    val mine: Set<Emoji> = emptySet(),
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val previews: List<LinkPreview> = emptyList(),
) {
    val preview = previews.firstOrNull()
}

data class FileToSend(val name: String, val mime: String, val contents: Base64Blob)

data class Attached(val name: String, val inline: Boolean)

data class Downloaded(val contents: Base64Blob, val type: String, val inline: Boolean)

data class Bookmarked(val title: String, val url: String)

data class Pinned(val author: String, val text: String)

data class ThreadSeen(val parent: Said, val replies: List<Said>)