package sl4k.view

import org.http4k.core.Uri
import org.http4k.fanbelt.input.goTo
import org.http4k.fanbelt.page.PageShell
import org.http4k.template.ViewModel
import sl4k.domain.Bookmark
import sl4k.domain.ChannelName
import sl4k.domain.GENERAL
import sl4k.domain.Emoji
import sl4k.domain.Message
import sl4k.domain.MessageId
import sl4k.domain.Upload
import sl4k.domain.Viewer
import sl4k.domain.ViewerId
import sl4k.domain.codeSpan
import sl4k.domain.isWebLink
import sl4k.domain.mention
import sl4k.domain.webLink

data class ChatSignals(val text: String = "", val name: String = "", val private: Boolean = false, val reply: String = "", val edit: String = "", val editing: String = "", val topic: String = "", val editingTopic: Boolean = false, val title: String = "", val url: String = "", val addingBookmark: Boolean = false, val canvas: String = "", val canvasBase: Int = 0, val editingCanvas: Boolean = false, val _canvasOpen: Boolean = false, val upload: List<Upload> = emptyList(), val _files: List<Upload> = emptyList())

data class ChannelLink(val name: ChannelName, val label: String, val unread: Int, val current: Boolean, val private: Boolean, val mentions: Int)

data class ChannelsView(val channels: List<ChannelLink>, val direct: List<ChannelLink>) : ViewModel

data class PersonRow(val id: ViewerId, val name: String, val here: Boolean, val you: Boolean)

data class PeopleView(val people: List<PersonRow>) : ViewModel

data class MembersView(val channel: ChannelName, val private: Boolean, val invitable: List<Viewer>, val removable: List<Viewer>) : ViewModel

data class TopicView(val channel: ChannelName, val topic: String) : ViewModel

data class BookmarksView(val channel: ChannelName, val bookmarks: List<Bookmark>) : ViewModel

data class PinRow(val id: MessageId, val author: String, val text: String, val path: String, val threadPath: String)

data class PinsView(val pins: List<PinRow>) : ViewModel

data class CanvasView(val channel: ChannelName, val text: String) : ViewModel

data class GateView(val allowed: Boolean) : ViewModel {
    val leave = goTo(Uri.of("c/$GENERAL"), id = "gate")
}

data class SuggestView(val channel: ChannelName, val suggestions: List<Viewer>) : ViewModel

data class Part(val text: String, val who: Viewer? = null, val style: Set<String> = emptySet(), val link: String? = null, val code: Boolean = false, val block: Boolean = false) {
    val classes = style.joinToString(" ")
}

data class AttachmentRow(val name: String, val path: String, val inline: Boolean)

data class PreviewRow(val url: String, val title: String, val description: String, val image: String?)

data class ReactionChip(val name: String, val glyph: String, val count: Int, val by: String)

data class MessageRow(
    val id: MessageId,
    val author: String,
    val authorId: ViewerId,
    val parts: List<Part>,
    val at: String,
    val path: String,
    val reactions: List<ReactionChip>,
    val edited: Boolean,
    val deleted: Boolean,
    val replies: Int = 0,
    val lastReply: String? = null,
    val threadPath: String? = null,
    val day: String? = null,
    val pinned: Boolean = false,
    val attachments: List<AttachmentRow> = emptyList(),
    val previews: List<PreviewRow> = emptyList(),
) {
    val replyCount = "$replies ${if (replies == 1) "reply" else "replies"}"
    val palette = Emoji.entries
}

fun chipsOf(reactions: Map<Emoji, Set<ViewerId>>) =
    reactions.entries.sortedBy { it.key.ordinal }.map { (emoji, by) -> ReactionChip(emoji.name, emoji.glyph, by.size, by.joinToString(" ")) }

data class ThreadView(val channel: ChannelName, val parent: MessageRow, val replies: List<MessageRow>) : ViewModel

data object ThreadGoneView : ViewModel

fun partsOf(message: Message): List<Part> {
    val mentioned = message.mentions.map(::Viewer).associateBy { it.handle }
    return formatted(message.text.value, emptySet(), mentioned)
}

private val marked = Regex(codeSpan + "|(" + webLink + ")|" + listOf("\\*", "_", "~").joinToString("|") { """(?<!\w)$it(\S|\S.*?\S)$it(?!\w)""" })

private fun formatted(text: String, styles: Set<String>, mentioned: Map<String, Viewer>): List<Part> =
    around(text, marked, { mentions(it, styles, mentioned) }) { match ->
        val (block, code, link, bold, italic, strike) = match.destructured
        when {
            block.isNotEmpty() -> listOf(Part(block, block = true))
            code.isNotEmpty() -> listOf(Part(code, code = true))
            link.isNotEmpty() -> listOf(Part(link, style = styles, link = link.takeIf(::isWebLink)))
            bold.isNotEmpty() -> formatted(bold, styles + "bold", mentioned)
            italic.isNotEmpty() -> formatted(italic, styles + "italic", mentioned)
            else -> formatted(strike, styles + "strike", mentioned)
        }
    }

private fun mentions(text: String, styles: Set<String>, mentioned: Map<String, Viewer>): List<Part> =
    around(text, mention, { listOf(Part(it, style = styles)) }) { match ->
        listOf(Part(match.value, mentioned[match.groupValues[1]], styles))
    }

private fun around(text: String, found: Regex, between: (String) -> List<Part>, matched: (MatchResult) -> List<Part>): List<Part> {
    val matches = found.findAll(text).toList()
    val plain = found.split(text).map { if (it.isEmpty()) emptyList() else between(it) }
    return plain.zip(matches.map(matched) + listOf(emptyList())).flatMap { (before, match) -> before + match }
}

data class HistoryView(val channel: ChannelName, val messages: List<MessageRow>, val older: Boolean, val more: Int) : ViewModel

data object ChannelGoneView : ViewModel

data class TypingView(val names: List<String>) : ViewModel {
    val verb = if (names.size == 1) "is" else "are"
    val separator = if (names.size == 2) " and " else ", "
}

data class Result(val channel: String, val row: MessageRow)

data class ResultsView(val query: String, val results: List<Result>) : ViewModel

data class SearchPageView(val shell: PageShell, val query: String, val viewer: ViewerId) : ViewModel

data class ChannelPageView(val shell: PageShell, val channel: ChannelName, val heading: String, val viewer: ViewerId, val firstUnread: MessageId?, val thread: MessageId?) : ViewModel