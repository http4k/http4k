package sl4k.feature

import dev.forkhandles.values.random
import org.http4k.fanbelt.store.Order.Newest
import org.http4k.fanbelt.store.ascending
import org.http4k.fanbelt.store.contains
import org.http4k.fanbelt.store.div
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.store.neq
import org.http4k.fanbelt.store.oneOf
import sl4k.domain.Attachment
import sl4k.domain.AttachmentContent
import sl4k.domain.AttachmentId
import sl4k.domain.Bookmark
import sl4k.domain.BookmarkId
import sl4k.domain.BookmarkTitle
import sl4k.domain.BookmarkUrl
import sl4k.domain.Canvas
import sl4k.domain.Channel
import sl4k.domain.ChannelName
import sl4k.domain.ChannelTopic
import sl4k.domain.Emoji
import sl4k.domain.Message
import sl4k.domain.MessageId
import sl4k.domain.MessageText
import sl4k.domain.MAX_RESULTS
import sl4k.domain.Preview
import sl4k.domain.PreviewId
import sl4k.domain.PreviewState.Pending
import sl4k.domain.PreviewState.Ready
import sl4k.domain.Reaction
import sl4k.domain.ReactionId
import sl4k.domain.Reactions
import sl4k.domain.ReadMark
import sl4k.domain.Reader
import sl4k.domain.ReplySummary
import sl4k.domain.Unfurled
import sl4k.domain.Unreadable
import sl4k.domain.linksIn
import sl4k.domain.Unread
import sl4k.domain.ValidFile
import sl4k.domain.Viewer
import sl4k.domain.ViewerId
import sl4k.domain.mention
import sl4k.store.ChatTables
import java.time.Clock
import java.util.Random

class Chat(private val tables: ChatTables, private val clock: Clock, private val random: Random) {
    private val live = Message::deletedAt eq null

    fun channels() = tables.channels.page(order = ascending(Channel::id)).items

    fun channel(name: ChannelName) = tables.channels.get(name)

    fun create(name: ChannelName, members: Set<ViewerId>? = null, direct: Boolean = false, owner: ViewerId? = null) =
        tables.channels.insert(Channel(name, clock.instant(), members, direct, owner))

    fun invite(channel: Channel, who: ViewerId) = tables.channels.update(channel.id) { it.copy(members = it.members.orEmpty() + who) }

    fun remove(channel: Channel, who: ViewerId) = tables.channels.update(channel.id) { it.copy(members = it.members.orEmpty() - who) }

    fun setTopic(channel: Channel, topic: ChannelTopic) = tables.channels.update(channel.id) { it.copy(topic = topic) }

    fun bookmarks(channel: ChannelName) = tables.bookmarks.page(Bookmark::channel eq channel).items

    fun bookmark(id: BookmarkId) = tables.bookmarks.get(id)

    fun addBookmark(channel: Channel, title: BookmarkTitle, url: BookmarkUrl) = tables.bookmarks.insert(Bookmark(BookmarkId.random(random), channel.id, title, url))

    fun removeBookmark(bookmark: Bookmark) = tables.bookmarks.delete(bookmark.id)

    fun canvas(channel: ChannelName) = tables.canvases.get(channel) ?: Canvas(channel)

    fun saveCanvas(canvas: Canvas) {
        if (tables.canvases.get(canvas.id) == null) tables.canvases.insert(canvas) else tables.canvases.update(canvas.id) { canvas }
    }

    fun deleteChannel(channel: ChannelName) {
        remove(tables.attachments.page(Attachment::channel eq channel).items)
        previews(channel).values.flatten().forEach { tables.previews.delete(it.id) }
        tables.messages.page(Message::channel eq channel).items.forEach { tables.messages.delete(it.id) }
        tables.reactions.page(Reaction::channel eq channel).items.forEach { tables.reactions.delete(it.id) }
        tables.marks.page((ReadMark::id / Reader::channel) eq channel).items.forEach { tables.marks.delete(it.id) }
        bookmarks(channel).forEach { tables.bookmarks.delete(it.id) }
        if (tables.canvases.get(channel) != null) tables.canvases.delete(channel)
        tables.channels.delete(channel)
    }

    fun teammates() = tables.marks.page().items.map { it.id.member }.distinct()

    fun knows(member: ViewerId) = tables.marks.count((ReadMark::id / Reader::member) eq member) > 0

    fun posted(channel: ChannelName) = tables.messages.count(inChannel(channel))

    fun latest(channel: ChannelName, show: Int) = tables.messages.page(inChannel(channel), Newest, limit = show).items.reversed()

    fun firstUnread(channel: ChannelName, member: ViewerId): MessageId? {
        val sinceMark = posted(channel) - (tables.marks.get(Reader(channel, member))?.count ?: 0)
        return if (sinceMark <= 0) null else latest(channel, sinceMark).firstOrNull { !it.deleted }?.id
    }

    fun search(channels: List<ChannelName>, query: String) =
        tables.messages.page((Message::channel oneOf channels) and (Message::text contains query) and live, Newest, limit = MAX_RESULTS).items

    fun message(channel: ChannelName, id: MessageId) = tables.messages.get(id)?.takeIf { it.channel == channel }

    fun liveMessage(channel: ChannelName, id: MessageId) = message(channel, id)?.takeUnless(Message::deleted)

    fun replies(parent: MessageId) = tables.messages.page((Message::thread eq parent) and live).items

    fun replyCounts(channel: ChannelName) = tables.messages.page((Message::channel eq channel) and (Message::thread neq null) and live).items
        .groupBy { it.thread!! }
        .mapValues { (_, replies) -> ReplySummary(replies.size, replies.maxOf { it.at }) }

    fun reactions(channel: ChannelName): Reactions = tables.reactions.page(Reaction::channel eq channel).items
        .groupBy { it.id.message }
        .mapValues { (_, reactions) -> reactions.groupBy { it.id.emoji }.mapValues { (_, by) -> by.map { it.id.member }.toSet() } }

    fun toggle(message: Message, emoji: Emoji, member: ViewerId) {
        val id = ReactionId(message.id, emoji, member)
        if (tables.reactions.get(id) == null) tables.reactions.insert(Reaction(id, message.channel)) else tables.reactions.delete(id)
    }

    fun post(channel: Channel, author: ViewerId, text: MessageText, thread: MessageId? = null, files: List<ValidFile> = emptyList()) {
        val message = Message(MessageId.random(random), channel.id, author, text, clock.instant(), mentionedIn(channel, text), thread)
        tables.messages.insert(message)
        files.forEach { attach(message, it) }
        if (thread == null) addPreviews(message, linksIn(text.value))
        if (thread == null) read(channel.id, author)
    }

    private fun watchLinks(message: Message, links: List<String>) {
        val watched = tables.previews.page((Preview::id / PreviewId::message) eq message.id).items.sortedBy { it.id.position }
        if (watched.map { it.url } == links) return
        watched.forEach { tables.previews.delete(it.id) }
        addPreviews(message, links)
    }

    private fun addPreviews(message: Message, links: List<String>) =
        links.forEachIndexed { position, link -> tables.previews.insert(Preview(PreviewId(message.id, position), message.channel, link)) }

    fun previews(channel: ChannelName) = tables.previews.page(Preview::channel eq channel).items.sortedBy { it.id.position }.groupBy { it.id.message }

    fun preview(id: PreviewId) = tables.previews.get(id)

    fun pendingPreviews() = tables.previews.page(Preview::state eq Pending).items

    fun unfurled(found: Unfurled) = tables.previews.get(found.preview)?.takeIf { it.url == found.url }?.also {
        tables.previews.update(it.id) { preview -> preview.copy(state = Ready, title = found.title, description = found.description, image = found.image) }
    }

    fun unreadable(link: Unreadable) {
        if (tables.previews.get(link.preview)?.url == link.url) tables.previews.delete(link.preview)
    }

    private fun attach(message: Message, file: ValidFile) {
        val id = AttachmentId.random(random)
        tables.attachments.insert(Attachment(id, message.id, message.channel, file.name, file.mime))
        tables.attachmentContents.insert(AttachmentContent(id, file.contents))
    }

    fun attachments(channel: ChannelName) = tables.attachments.page(Attachment::channel eq channel).items.groupBy { it.message }

    fun attachment(id: AttachmentId) = tables.attachments.get(id)

    fun contents(id: AttachmentId) = tables.attachmentContents.get(id)?.contents

    private fun remove(attachments: List<Attachment>) = attachments.forEach {
        tables.attachmentContents.delete(it.id)
        tables.attachments.delete(it.id)
    }

    fun edit(channel: Channel, message: Message, text: MessageText) {
        tables.messages.update(message.id) { it.copy(text = text, mentions = mentionedIn(channel, text), editedAt = clock.instant()) }
        if (message.thread == null) watchLinks(message, linksIn(text.value))
    }

    fun delete(message: Message) {
        remove(tables.attachments.page(Attachment::message eq message.id).items)
        watchLinks(message, emptyList())
        tables.messages.update(message.id) { it.copy(deletedAt = clock.instant()) }
    }

    fun pins(channel: ChannelName) = tables.messages.page((Message::channel eq channel) and (Message::pinnedAt neq null) and live).items

    fun togglePin(message: Message) = tables.messages.update(message.id) { it.copy(pinnedAt = if (it.pinnedAt == null) clock.instant() else null) }

    fun unread(channel: ChannelName, since: Map<ViewerId, Int>): Map<ViewerId, Unread> {
        val most = since.values.maxOrNull() ?: 0
        val latest = if (most <= 0) emptyList() else latest(channel, most)
        return since.mapValues { (member, sinceMark) ->
            val unread = if (sinceMark <= 0) emptyList() else latest.takeLast(sinceMark).filterNot { it.deleted }
            Unread(unread.size, unread.count { member in it.mentions })
        }
    }

    private fun mentionedIn(channel: Channel, text: MessageText): Set<ViewerId> {
        val handles = mention.findAll(text.value).map { it.groupValues[1] }.toSet()
        if (handles.isEmpty()) return emptySet()
        return teammates().filter { channel.visibleTo(it) && Viewer(it).handle in handles }.toSet()
    }

    private fun inChannel(channel: ChannelName) = (Message::channel eq channel) and (Message::thread eq null)

    fun marks() = tables.marks.page().items.groupBy { it.id.member }.mapValues { (_, marks) -> marks.associate { it.id.channel to it.count } }

    fun read(channel: ChannelName, member: ViewerId) {
        val mark = ReadMark(Reader(channel, member), posted(channel))
        if (tables.marks.get(mark.id) == null) tables.marks.insert(mark) else tables.marks.update(mark.id) { mark }
    }
}