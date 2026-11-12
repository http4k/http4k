package sl4k.web

import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.page.Region
import org.http4k.routing.path
import org.http4k.template.TemplateRenderer
import sl4k.domain.SearchKey
import sl4k.domain.Preview
import sl4k.domain.Message
import sl4k.domain.PAGE
import sl4k.domain.Reactions
import sl4k.domain.SidebarChannel
import sl4k.domain.ThreadKey
import sl4k.domain.Viewer
import sl4k.domain.ViewerId
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.view.AttachmentRow
import sl4k.view.BookmarksView
import sl4k.view.CanvasView
import sl4k.view.ChannelGoneView
import sl4k.view.ChannelLink
import sl4k.view.ChannelsView
import sl4k.view.GateView
import sl4k.view.HistoryView
import sl4k.view.MembersView
import sl4k.view.MessageRow
import sl4k.view.PeopleView
import sl4k.view.PinRow
import sl4k.view.PinsView
import sl4k.view.PersonRow
import sl4k.view.ThreadGoneView
import sl4k.view.ThreadView
import sl4k.view.TopicView
import sl4k.view.TypingView
import sl4k.view.chipsOf
import sl4k.view.PreviewRow
import sl4k.view.Result
import sl4k.view.ResultsView
import sl4k.view.partsOf
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

private val clockTime = DateTimeFormatter.ofPattern("HH:mm").withZone(UTC)
private val calendarDay = DateTimeFormatter.ofPattern("EEE d MMM yyyy", ENGLISH).withZone(UTC)

private fun messagePathOf(message: Message) = "c/${message.channel}/m/${message.id}"

private fun threadPathOf(message: Message) = "c/${message.channel}/t/${message.thread ?: message.id}"

private fun rowOf(message: Message, reactions: Reactions) = MessageRow(
    message.id,
    Viewer(message.author).name,
    message.author,
    if (message.deleted) emptyList() else partsOf(message),
    clockTime.format(message.at),
    messagePathOf(message),
    if (message.deleted) emptyList() else chipsOf(reactions[message.id].orEmpty()),
    message.editedAt != null,
    message.deleted,
)

private fun previewsOf(previews: List<Preview>) = previews.map { PreviewRow(it.url, it.title, it.description, if (it.image.isEmpty()) null else "c/${it.channel}/previews/${it.id.message}/${it.id.position}/image") }

class ChatRegions(chat: ChatFeature, renderer: TemplateRenderer) {
    val channels = Region("channels", renderer, chat.sidebars) { sidebars, request ->
        val (open, direct) = sidebars.of(principal(request).id).partition { it.with == null }
        fun link(channel: SidebarChannel, label: String) = ChannelLink(channel.name, label, channel.unread, channel.name.value == request.path("channel"), channel.private, channel.mentions)
        ChannelsView(open.map { link(it, it.name.value) }, direct.map { link(it, Viewer(it.with!!).name) })
    }
    val people = Region("people", renderer, chat.roster) { roster, request ->
        val you = principal(request).id
        val rows = roster.teammates.map { PersonRow(it, Viewer(it).name, it in roster.here, it == you) }
        PeopleView(rows.sortedWith(compareBy({ !it.you }, { !it.here }, { it.name })))
    }
    val members = Region("members", renderer, channelName, chat.membership) { membership, request ->
        fun people(ids: Collection<ViewerId>) = ids.map(::Viewer).sortedBy { it.name }
        val owner = membership.owner == principal(request).id
        MembersView(channelName(request), membership.private, people(membership.invitable), if (owner) people(membership.members - principal(request).id) else emptyList())
    }
    val topic = Region("topic", renderer, channelName, chat.channelTopic) { topic, request -> TopicView(channelName(request), topic) }
    val bookmarks = Region("bookmarks", renderer, channelName, chat.bookmarks) { bookmarks, request -> BookmarksView(channelName(request), bookmarks) }
    val pins = Region("pins", renderer, channelName, chat.pins) { pins, _ -> PinsView(pins.map { PinRow(it.id, Viewer(it.author).name, it.text.value, messagePathOf(it), threadPathOf(it)) }) }
    val canvas = Region("canvas", renderer, channelName, chat.canvas) { canvas, request -> CanvasView(channelName(request), canvas.text.value) }
    val gate = Region("gate", renderer, channelAttempt, chat.visibleChannel) { access, _ -> GateView(access is Allowed) }
    val history = Region("history", renderer, { request -> Window(channelName(request), show(request).coerceIn(PAGE, 1000)) }, chat.history, fallback = ChannelGoneView) { history, _ ->
        if (history == null) return@Region ChannelGoneView
        val days = history.messages.map { calendarDay.format(it.at) }
        val rows = history.messages.mapIndexed { index, message ->
            val replied = history.replies[message.id]
            rowOf(message, history.reactions).copy(replies = replied?.count ?: 0, lastReply = replied?.let { clockTime.format(it.last) }, threadPath = threadPathOf(message), day = days[index].takeIf { index == 0 || it != days[index - 1] }, previews = previewsOf(history.previews[message.id].orEmpty()), pinned = message.pinnedAt != null, attachments = history.attachments[message.id].orEmpty().map { AttachmentRow(it.name.value, "c/${it.channel}/files/${it.id}", it.inline) })
        }
        HistoryView(history.channel, rows.reversed(), history.older, history.messages.size + PAGE)
    }
    val results = Region("results", renderer, { request -> SearchKey(principal(request).id, query(request)) }, chat.found) { hits, request ->
        ResultsView(query(request).trim(), hits.map { Result(it.label, rowOf(it.message, emptyMap()).copy(threadPath = threadPathOf(it.message))) })
    }
    val typing = Region("typing", renderer, channelName, chat.typing) { people, request ->
        TypingView((people - principal(request).id).map { Viewer(it).name }.sorted())
    }
    val thread = Region("thread", renderer, { request -> ThreadKey(channelName(request), messageId(request)) }, chat.thread, fallback = ThreadGoneView) { thread, _ ->
        if (thread == null) return@Region ThreadGoneView
        ThreadView(thread.parent.channel, rowOf(thread.parent, thread.reactions).copy(replies = thread.replies.size), thread.replies.map { rowOf(it, thread.reactions) })
    }
    val channelPage = listOf(channels, people, history, topic, bookmarks, pins, canvas, members, typing, gate)
}