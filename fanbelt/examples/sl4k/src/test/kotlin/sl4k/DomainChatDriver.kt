package sl4k

import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.state.join
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import sl4k.domain.AttachmentRef
import sl4k.domain.BookmarkRef
import sl4k.domain.CanvasEdit
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.Edit
import sl4k.domain.Emoji
import sl4k.domain.Message
import sl4k.domain.MessageId
import sl4k.domain.MessageRef
import sl4k.domain.MessageText
import sl4k.domain.NewBookmark
import sl4k.domain.NewChannel
import sl4k.domain.PAGE
import sl4k.domain.Preview
import sl4k.domain.React
import sl4k.domain.Reader
import sl4k.domain.Reply
import sl4k.domain.SearchKey
import sl4k.domain.Seen
import sl4k.domain.ThreadKey
import sl4k.domain.TopicChange
import sl4k.domain.Upload
import sl4k.domain.Viewer
import sl4k.domain.ViewerId
import sl4k.domain.Window
import sl4k.domain.directChannel
import sl4k.feature.ChatFeature
import sl4k.store.ChatTables
import sl4k.store.Teams
import java.time.Duration

class DomainChatDriver : ChatDriver {
    private val tenant = TestTenant(TestRuntime(http = fakeSites))
    private val chat = ChatFeature(tenant.runtime)
    private val general = ChannelName.of("general")

    init {
        tenant.runs(chat.unfurling.unfurl)
        Teams(tenant.storage).create(tenant.id, tenant.clock, general, ChannelName.of("random"))
    }

    override fun teammate(name: String): Teammate = DomainTeammate(viewerNamed(name))

    private fun said(message: Message, me: ViewerId, reactions: Map<MessageId, Map<Emoji, Set<ViewerId>>>, replies: Int = 0, previews: List<Preview> = emptyList()): Said {
        val reacted = reactions[message.id].orEmpty()
        if (message.deleted) return Said(Viewer(message.author).name, MessageText.of("This message was deleted"), replies = replies, deleted = true)
        return Said(
            Viewer(message.author).name,
            message.text,
            message.mentions.map { Viewer(it).name }.toSet(),
            replies,
            reacted.mapValues { it.value.size },
            reacted.filterValues { me in it }.keys,
            message.editedAt != null,
            previews = previews.map { LinkPreview(it.title, it.description, it.image.isNotEmpty()) },
        )
    }

    private fun idOf(text: MessageText) = tenant.tables { ChatTables(it).messages.page().items.first { it.text == text }.id }

    private fun bookmarkNamed(title: String) = tenant.tables { ChatTables(it).bookmarks.page().items.first { it.title.value == title } }

    override fun tick(by: Duration) = tenant.tick(by)

    private inner class DomainTeammate(override val id: ViewerId) : Teammate {
        override val name = Viewer(id).name
        private val member = Viewer(id)
        private val windows = mutableMapOf<ChannelName, Int>()
        private var inside: AutoCloseable? = null
        private var on: ChannelName? = general

        private fun sidebar() = tenant { chat.sidebar.read(id).map { if (inside != null && it.name == on) it.copy(unread = 0, mentions = 0) else it } }
        private var basedOn: Int? = null
        private var divider: MessageId? = null

        private fun leaves() {
            on?.takeIf { inside != null }?.let { tenant { chat.read.execute(member, Seen(it, chat.posted.read(it))) } }
            inside?.close()
        }

        override fun viewing() = on?.takeIf { tenant { chat.visibleChannel.read(ActorKey(Viewer(id), it)) is Allowed } }

        override fun online() = tenant { chat.roster.read().here.map { Viewer(it).name }.toSet() }

        override fun closes() {
            leaves()
            inside = null
            tenant.settle()
        }

        override fun you() = tenant { chat.roster.read().teammates.single { it == id }.let { Viewer(it).name } }

        override val channels: Channels = object : Channels {
            override fun searches(query: String): List<Found> {
                leaves()
                inside = chat.online.join(tenant.scope, id)
                on = null
                return tenant { chat.found.read(SearchKey(id, query)).map { Found(it.label, Viewer(it.message.author).name, it.message.text) } }
            }

            override fun open(channel: ChannelName): OpenChannel {
                val opened = channelOn(channel)
                val here = on == channel && inside != null
                if (!here && opened.canRead() && tenant { chat.history.read(Window(channel, PAGE)) != null }) {
                    divider = tenant { chat.firstUnread.read(Reader(channel, member.id)) }
                    leaves()
                    inside = chat.enter(tenant.scope, member, channel)
                    on = channel
                    opened.sees()
                    list()
                }
                return opened
            }

            override fun creates(channel: String) = tenant { chat.create.execute(member, NewChannel(channel, private = false)) }

            override fun createsPrivate(channel: ChannelName) = tenant { chat.create.execute(member, NewChannel(channel.value, private = true)) }

            override fun list() = sidebar().filter { it.with == null }.map { it.name }

            override fun unread() = sidebar().filter { it.unread > 0 }.associate { it.name to it.unread }

            override fun mentions() = sidebar().filter { it.mentions > 0 }.associate { it.name to it.mentions }

            override fun messagesDirectly(who: Teammate): ChannelName {
                tenant { chat.direct.execute(member, who.id) }
                return directChannel(id, who.id)
            }

            override fun directMessages() = sidebar().filter { it.with != null }.associate { Viewer(it.with!!).name to it.unread }
        }

        private fun channelOn(channel: ChannelName): OpenChannel = object : OpenChannel {
            override val topic = topicOn(channel)
            override val bookmarks = bookmarksOn(channel)
            override val canvas = canvasOn(channel)

            override fun message(text: MessageText) = messageOn(channel, text)

            override fun attachesAll(files: List<FileToSend>, text: String) = tenant { chat.post.execute(member, Draft(channel, text, files.map { file -> Upload(file.name, file.mime, file.contents.value) })) }

            override fun downloads(file: String): Downloaded? {
                val found = tenant.tables { ChatTables(it).attachments.page().items.firstOrNull { it.name.value == file } } ?: return null
                val download = chat.download(tenant.scope, member, AttachmentRef(channel, found.id)) ?: return null
                return Downloaded(download.contents, download.attachment.servedAs, download.attachment.inline)
            }

            override fun posts(text: String) = tenant { chat.post.execute(member, Draft(channel, text)) }

            override fun sees(): ChannelSeen {
                val history = tenant { chat.history.read(Window(channel, windows[channel] ?: PAGE))!! }
                return ChannelSeen(history.messages.map { said(it, id, history.reactions, history.replies[it.id]?.count ?: 0, history.previews[it.id]?.links.orEmpty()) }, history.older)
            }

            override fun firstUnread() = tenant { chat.history.read(Window(channel, windows[channel] ?: PAGE))!!.messages.firstOrNull { it.id == divider }?.text?.value }

            override fun showsOlder() {
                windows[channel] = (windows[channel] ?: PAGE) + PAGE
            }

            override fun canRead() = tenant.scope.read(chat.visibleChannel, ActorKey(Viewer(id), channel)) is Allowed

            override fun heading() = chat.heading(tenant.scope, member, channel)

            override fun invites(who: Teammate) = tenant { chat.invite.execute(member, Reader(channel, who.id)) }

            override fun removes(who: Teammate) = tenant { chat.remove.execute(member, Reader(channel, who.id)) }

            override fun leaves() = tenant { chat.leave.execute(member, channel) }

            override fun types(text: String) = tenant { chat.here.carry(channel, id, chat.typing()) }

            override fun typing() = tenant { (chat.typing.read(channel) - id).map { Viewer(it).name }.toSet() }

            override fun suggestions(typed: String) = chat.suggestions(tenant.scope, member, channel, typed).map { Viewer(it).name }.toSet()

            override fun tabs(typed: String) = chat.complete(tenant.scope, member, channel, typed)

            override fun pinned() = tenant { chat.pins.read(channel).map { Pinned(Viewer(it.author).name, it.text.value) } }
        }

        private fun topicOn(channel: ChannelName): TopicLine = object : TopicLine {
            override fun sets(text: String) = tenant { chat.setTopic.execute(member, TopicChange(channel, text)) }

            override fun read() = tenant { chat.channelTopic.read(channel) }
        }

        private fun bookmarksOn(channel: ChannelName): BookmarkBar = object : BookmarkBar {
            override fun adds(title: String, url: String) = tenant { chat.addBookmark.execute(member, NewBookmark(channel, title, url)) }

            override fun removes(title: String) = tenant { chat.removeBookmark.execute(member, BookmarkRef(channel, bookmarkNamed(title).id)) }

            override fun list() = tenant { chat.bookmarks.read(channel).map { Bookmarked(it.title.value, it.url.value) } }
        }

        private fun canvasOn(channel: ChannelName): CanvasPanel = object : CanvasPanel {
            override fun begins() {
                basedOn = tenant { chat.canvas.read(channel).version }
            }

            override fun saves(text: String): Verdict {
                val base = basedOn ?: tenant { chat.canvas.read(channel) }.version
                val verdict = tenant { chat.saveCanvas.execute(member, CanvasEdit(channel, text, base)) }
                basedOn = if (verdict == Verdict.Accepted) null else base
                return verdict
            }

            override fun read() = tenant { chat.canvas.read(channel).text.value }
        }

        private fun messageOn(channel: ChannelName, said: MessageText): PostedMessage = object : PostedMessage {
            override fun opensThread() {
                channels.open(channel)
                thread()
            }

            override fun thread(): ThreadSeen {
                val thread = tenant { chat.thread.read(ThreadKey(channel, idOf(said)))!! }
                return ThreadSeen(said(thread.parent, id, thread.reactions, thread.replies.size), thread.replies.map { said(it, id, thread.reactions) })
            }

            override fun replies(text: String) = tenant { chat.reply.execute(member, Reply(channel, idOf(said), text)) }

            override fun reacts(emoji: Emoji) = tenant { chat.react.execute(member, React(channel, idOf(said), emoji.name)) }

            override fun edits(to: String) = tenant { chat.edit.execute(member, Edit(channel, idOf(said), to)) }

            override fun deletes() = tenant { chat.delete.execute(member, MessageRef(channel, idOf(said))) }

            override fun attachments() = tenant { chat.attachments.read(channel)[idOf(said)].orEmpty().map { Attached(it.name.value, it.inline) } }

            override fun togglesPin() = tenant { chat.togglePin.execute(member, MessageRef(channel, idOf(said))) }

            override fun unpinsFromTheTop() = togglesPin()
        }
    }
}
