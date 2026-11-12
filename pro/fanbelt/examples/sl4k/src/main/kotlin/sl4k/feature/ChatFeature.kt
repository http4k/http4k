package sl4k.feature

import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.feature.Access.Hidden
import org.http4k.fanbelt.feature.Access.Denied
import org.http4k.fanbelt.feature.Access.Unavailable
import org.http4k.fanbelt.feature.Attempt
import org.http4k.fanbelt.feature.orNull
import org.http4k.fanbelt.feature.then
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.join
import org.http4k.fanbelt.state.topic
import org.http4k.fanbelt.tenancy.Scope
import sl4k.domain.AttachmentRef
import sl4k.domain.BookmarkRef
import sl4k.domain.BookmarkTitle
import sl4k.domain.BookmarkUrl
import sl4k.domain.CanvasEdit
import sl4k.domain.CanvasText
import sl4k.domain.ChannelMessage
import sl4k.domain.Channel
import sl4k.domain.ChannelName
import sl4k.domain.ChannelTopic
import sl4k.domain.Download
import sl4k.domain.Draft
import sl4k.domain.Edit
import sl4k.domain.Emoji
import sl4k.domain.History
import sl4k.domain.MAX_CANVAS
import sl4k.domain.MAX_FILES
import sl4k.domain.MAX_TOPIC
import sl4k.domain.PreviewState.Ready
import sl4k.domain.Viewer
import sl4k.domain.Membership
import sl4k.domain.Hit
import sl4k.domain.MessageId
import sl4k.domain.MessageRef
import sl4k.domain.SearchKey
import sl4k.domain.PreviewId
import sl4k.domain.MessageText
import sl4k.domain.NewBookmark
import sl4k.domain.NewChannel
import sl4k.domain.React
import sl4k.domain.PreviewRef
import sl4k.domain.Reader
import sl4k.domain.Reply
import sl4k.domain.Roster
import sl4k.domain.SidebarChannel
import sl4k.domain.Sidebars
import sl4k.domain.TYPING_FOR
import sl4k.domain.Thread
import sl4k.domain.ThreadKey
import sl4k.domain.TopicChange
import sl4k.domain.Typing
import sl4k.domain.Unread
import sl4k.domain.ViewerId
import sl4k.domain.Window
import sl4k.domain.completed
import sl4k.domain.bookmarkField
import sl4k.domain.canvasField
import sl4k.domain.directChannel
import sl4k.domain.editField
import sl4k.domain.handleBeingTyped
import sl4k.domain.nameField
import sl4k.domain.pinField
import sl4k.domain.reactionField
import sl4k.domain.replyField
import sl4k.domain.textField
import sl4k.domain.topicField
import sl4k.domain.uploadField
import sl4k.store.ChatTables
import java.time.Clock
import java.util.Random

class ChatFeature(private val clock: Clock, random: Random) : Feature<Chat, Viewer>({ tables -> Chat(ChatTables(tables), clock, random) }) {
    val channels by state { chat -> chat.channels() }

    private fun channelNamed(ctx: StateContext<Chat>, name: ChannelName): Channel? {
        ctx.watch(channels.topic())
        return ctx.domain.channel(name)
    }

    val posted by state(ChannelName) { chat, channel -> chat.posted(channel) }
    val marks by state { chat -> chat.marks() }
    val here by presence(ChannelName, ViewerId, Typing::class)
    val online by presence(ViewerId)

    val typing by derived { ctx, channel: ChannelName ->
        val now = clock.instant()
        val typing = ctx.read(here, channel).filterValues { it != null && it.until > now }
        typing.values.filterNotNull().forEach { ctx.staleAt(it.until) }
        typing.keys
    }

    val teammates by derived { ctx, _: Unit -> ctx.read(marks).keys.toList() }

    val roster by derived { ctx, _: Unit -> Roster(ctx.read(teammates), ctx.read(online).keys) }

    val visibleChannel by rule { ctx, viewer, name: ChannelName ->
        val channel = channelNamed(ctx, name)
        if (channel?.visibleTo(viewer.id) == true) Allowed(channel) else Hidden
    }

    val yourMessage by rule { ctx, viewer, ref: MessageRef ->
        ctx.read(visibleChannel, Attempt(viewer, ref.channel)).then { channel ->
            val message = ctx.domain.liveMessage(ref.channel, ref.message)
            when {
                message == null -> Unavailable(editField, "No such message")
                message.author != viewer.id -> Denied
                else -> Allowed(ChannelMessage(channel, message))
            }
        }
    }

    val groupChannel by rule { ctx, viewer, name: ChannelName ->
        ctx.read(visibleChannel, Attempt(viewer, name)).then { channel -> if (ctx.read(membership, name).private) Allowed(channel) else Denied }
    }

    val yourChannel by rule { ctx, viewer, name: ChannelName ->
        ctx.read(visibleChannel, Attempt(viewer, name)).then { channel -> if (channel.owner == viewer.id) Allowed(channel) else Denied }
    }

    val visibleFile by rule { ctx, viewer, ref: AttachmentRef ->
        ctx.read(visibleChannel, Attempt(viewer, ref.channel)).then {
            val attachment = ctx.domain.attachment(ref.attachment)?.takeIf { it.channel == ref.channel }
            val contents = attachment?.run { ctx.domain.contents(id) }
            if (attachment == null || contents == null) Hidden else Allowed(Download(attachment, contents))
        }
    }

    val visibleImage by rule { ctx, viewer, ref: PreviewRef ->
        ctx.read(visibleChannel, Attempt(viewer, ref.channel)).then {
            val image = ctx.domain.preview(ref.preview)?.takeIf { it.channel == ref.channel }?.image?.ifEmpty { null }
            if (image == null) Hidden else Allowed(image)
        }
    }

    val mentionable by derived { ctx, name: ChannelName ->
        val channel = channelNamed(ctx, name)
        if (channel == null) emptyList() else ctx.read(teammates).filter(channel::visibleTo)
    }

    val membership by derived { ctx, name: ChannelName ->
        val channel = channelNamed(ctx, name)
        if (channel?.members == null || channel.direct) Membership(false, null, emptySet(), emptyList())
        else Membership(true, channel.owner, channel.members, ctx.read(teammates) - channel.members)
    }

    val channelTopic by derived { ctx, name: ChannelName -> channelNamed(ctx, name)?.topic?.value.orEmpty() }

    val bookmarks by state(ChannelName) { chat, channel -> chat.bookmarks(channel) }

    val canvas by state(ChannelName) { chat, channel -> chat.canvas(channel) }

    val sidebars by derived { ctx, _: Unit ->
        val marks = ctx.read(marks)
        val channels = ctx.read(channels)
        val everyone = (ctx.read(teammates) + channels.flatMap { it.members.orEmpty() }).toSet()
        val unread = channels.associate { channel ->
            val posted = ctx.read(posted, channel.id)
            val since = everyone.filter { channel.visibleTo(it) }.associateWith { posted - (marks[it]?.get(channel.id) ?: 0) }
            channel.id to ctx.domain.unread(channel.id, since)
        }
        val members = everyone.associateWith { member ->
            channels.filter { it.visibleTo(member) }.map { channel ->
                val counted = unread.getValue(channel.id)[member] ?: Unread(0, 0)
                SidebarChannel(channel.id, counted.messages, channel.peerOf(member), channel.members != null, counted.mentions)
            }
        }
        Sidebars(members, channels.filter { it.members == null }.map { SidebarChannel(it.id, 0, null, false, 0) })
    }

    val found by derived { ctx, key: SearchKey ->
        val query = key.query.trim()
        val visible = ctx.read(channels).filter { it.visibleTo(key.member) }
        visible.forEach { ctx.watch(posted.topic(it.id), replies.topic(it.id)) }
        val labels = visible.associate { it.id to it.label(key.member) }
        if (query.isEmpty()) emptyList() else ctx.domain.search(labels.keys.toList(), query).map { Hit(it, labels.getValue(it.channel)) }
    }

    val replies by state(ChannelName) { chat, channel -> chat.replyCounts(channel) }

    val reactions by state(ChannelName) { chat, channel -> chat.reactions(channel) }

    val pins by state(ChannelName) { chat, channel -> chat.pins(channel) }

    val attachments by state(ChannelName) { chat, channel -> chat.attachments(channel) }

    val previews by state(ChannelName) { chat, channel -> chat.previews(channel) }

    val history by derived { ctx, window: Window ->
        ctx.watch(posted.topic(window.channel), pins.topic(window.channel))
        val replied = ctx.read(replies, window.channel)
        val latest = ctx.domain.latest(window.channel, window.show + 1)
        if (ctx.domain.channel(window.channel) == null) null
        else History(window.channel, latest.takeLast(window.show).filter { !it.deleted || it.id in replied }, latest.size > window.show, replied, ctx.read(reactions, window.channel), ctx.read(attachments, window.channel), ctx.read(previews, window.channel).mapValues { (_, found) -> found.filter { it.state == Ready } })
    }

    val thread by derived { ctx, key: ThreadKey ->
        ctx.watch(replies.topic(key.channel))
        val reacted = ctx.read(reactions, key.channel)
        val parent = ctx.domain.message(key.channel, key.parent)
        if (parent == null || parent.thread != null) null else Thread(parent, ctx.domain.replies(parent.id), reacted)
    }

    val post by operation(visibleChannel, Draft::channel) { chat, member, draft, channel ->
        if (draft.files.size > MAX_FILES) return@operation Rejected(uploadField, "Send up to $MAX_FILES files at once")
        val files = draft.files.map { file -> file.orElse { return@operation it } }.sortedBy { it.name.value }
        val text = textField.check(MessageText, draft.text.ifBlank { files.firstOrNull()?.name?.value.orEmpty() }, "Say something first").orElse { return@operation it }
        chat.post(channel, member.id, text, files = files)
        Changes.of(posted.topic(draft.channel), marks.topic())
    }

    val reply by operation(visibleChannel, Reply::channel) { chat, member, reply, channel ->
        val parent = chat.message(reply.channel, reply.parent) ?: return@operation Rejected(replyField, "No such message")
        if (parent.thread != null) return@operation Rejected(replyField, "Threads are one level deep")
        val text = replyField.check(MessageText, reply.text, "Say something first").orElse { return@operation it }
        chat.post(channel, member.id, text, thread = parent.id)
        Changes.of(replies.topic(reply.channel))
    }

    val react by operation(visibleChannel, React::channel) { chat, member, react, _ ->
        val message = chat.message(react.channel, react.message) ?: return@operation Rejected(reactionField, "No such message")
        val emoji = reactionField.check(Emoji.entries, react.emoji, "No such reaction").orElse { return@operation it }
        chat.toggle(message, emoji, member.id)
        Changes.of(reactions.topic(react.channel))
    }

    val edit by operation(yourMessage, { edit: Edit -> MessageRef(edit.channel, edit.message) }) { chat, _, edit, (channel, message) ->
        val text = editField.check(MessageText, edit.text, "Say something first").orElse { return@operation it }
        chat.edit(channel, message, text)
        Changes.of(posted.topic(edit.channel), replies.topic(edit.channel), pins.topic(edit.channel), previews.topic(edit.channel))
    }

    val delete by operation(yourMessage) { chat, _, ref, (_, message) ->
        chat.delete(message)
        Changes.of(posted.topic(ref.channel), replies.topic(ref.channel), pins.topic(ref.channel), attachments.topic(ref.channel), previews.topic(ref.channel))
    }

    val togglePin by operation(visibleChannel, MessageRef::channel) { chat, _, ref, _ ->
        val message = chat.liveMessage(ref.channel, ref.message) ?: return@operation Rejected(pinField, "No such message")
        if (message.thread != null) return@operation Rejected(pinField, "Only channel messages can be pinned")
        chat.togglePin(message)
        Changes.of(pins.topic(ref.channel))
    }

    val saveCanvas by operation(visibleChannel, CanvasEdit::channel) { chat, _, edit, _ ->
        val text = canvasField.check(CanvasText, edit.text, "Keep the canvas to $MAX_CANVAS characters").orElse { return@operation it }
        val saved = chat.canvas(edit.channel)
        if (saved.version != edit.basedOn) return@operation Rejected(canvasField, "A teammate saved first. Copy your text, then edit again")
        chat.saveCanvas(saved.copy(text = text, version = saved.version + 1))
        Changes.of(canvas.topic(edit.channel))
    }

    fun download(scope: Scope, member: Viewer, ref: AttachmentRef) = scope.read(visibleFile, Attempt(member, ref)).orNull()

    fun previewImage(scope: Scope, member: Viewer, channel: ChannelName, preview: PreviewId) = scope.read(visibleImage, Attempt(member, PreviewRef(channel, preview))).orNull()

    fun draft(scope: Scope, member: Viewer, ref: MessageRef) = scope.read(yourMessage, Attempt(member, ref)).orNull()?.message?.text?.value

    val setTopic by operation(visibleChannel, TopicChange::channel) { chat, _, change, channel ->
        val topic = topicField.check(ChannelTopic, change.text, "Keep the topic to $MAX_TOPIC characters").orElse { return@operation it }
        chat.setTopic(channel, topic)
        Changes.of(channels.topic())
    }

    val addBookmark by operation(visibleChannel, NewBookmark::channel) { chat, _, new, channel ->
        val title = bookmarkField.check(BookmarkTitle, new.title, "Give the bookmark a title").orElse { return@operation it }
        val url = bookmarkField.check(BookmarkUrl, new.url, "Use an http or https link").orElse { return@operation it }
        chat.addBookmark(channel, title, url)
        Changes.of(bookmarks.topic(new.channel))
    }

    val removeBookmark by operation(visibleChannel, BookmarkRef::channel) { chat, _, ref, _ ->
        val bookmark = chat.bookmark(ref.bookmark)?.takeIf { it.channel == ref.channel } ?: return@operation Rejected(bookmarkField, "No such bookmark")
        chat.removeBookmark(bookmark)
        Changes.of(bookmarks.topic(ref.channel))
    }

    val create by operation { chat, member, new: NewChannel ->
        val name = nameField.check(ChannelName, new.name.trim().removePrefix("#"), "Use lowercase letters, numbers and dashes").orElse { return@operation it }
        if (name.value.startsWith("dm-")) return@operation Rejected(nameField, "Names starting dm- are for direct messages")
        if (chat.channel(name) != null) return@operation Rejected(nameField, "#$name already exists")
        if (new.private) chat.create(name, setOf(member.id), owner = member.id) else chat.create(name)
        Changes.of(setOf(channels.topic()), name)
    }

    val read by operation(visibleChannel) { chat, member, name, _ ->
        chat.read(name, member.id)
        Changes.of(marks.topic())
    }

    val invite by operation(groupChannel, Reader::channel) { chat, _, invite, channel ->
        if (!chat.knows(invite.member)) return@operation Rejected(textField, "No such teammate")
        chat.invite(channel, invite.member)
        Changes.of(channels.topic())
    }

    val leave by operation(groupChannel) { chat, member, name, channel ->
        if (channel.members == setOf(member.id)) {
            chat.deleteChannel(name)
            return@operation Changes.of(channels.topic(), posted.topic(name), replies.topic(name), reactions.topic(name), bookmarks.topic(name), pins.topic(name), canvas.topic(name), previews.topic(name), marks.topic(), attachments.topic(name))
        }
        chat.remove(channel, member.id)
        Changes.of(channels.topic())
    }

    val remove by operation(yourChannel, Reader::channel) { chat, member, removal, channel ->
        if (removal.member == member.id) return@operation Rejected(nameField, "Leave #${channel.id} instead")
        if (removal.member !in channel.members.orEmpty()) return@operation Rejected(nameField, "They aren't in #${channel.id}")
        chat.remove(channel, removal.member)
        Changes.of(channels.topic())
    }

    val direct by operation { chat, member, with: ViewerId ->
        if (with == member.id || !chat.knows(with)) return@operation Rejected(textField, "No such teammate")
        val name = directChannel(member.id, with)
        if (chat.channel(name) == null) chat.create(name, setOf(member.id, with), direct = true)
        Changes.of(setOf(channels.topic()), name)
    }

    fun heading(scope: Scope, member: Viewer, channel: ChannelName) =
        scope.read(visibleChannel, Attempt(member, channel)).orNull()?.label(member.id) ?: "#$channel"

    fun firstUnread(scope: Scope, member: Viewer, channel: ChannelName): MessageId? =
        if (member.id in scope.read(here, channel)) null else scope.read(this) { chat -> chat.firstUnread(channel, member.id) }

    fun suggestions(scope: Scope, member: Viewer, channel: ChannelName, text: String): List<ViewerId> {
        val typed = handleBeingTyped(text) ?: return emptyList()
        return (scope.read(mentionable, channel) - member.id).filter { Viewer(it).handle.startsWith(typed) }.sortedBy { Viewer(it).handle }
    }

    fun complete(scope: Scope, member: Viewer, channel: ChannelName, text: String) =
        suggestions(scope, member, channel, text).firstOrNull()?.let { completed(text, Viewer(it).handle) } ?: text

    fun typing() = Typing(clock.instant() + TYPING_FOR)

    fun enter(scope: Scope, member: Viewer, channel: ChannelName): AutoCloseable {
        scope.execute(read, member, channel)
        val lease = here.join(scope, channel, member.id)
        val present = online.join(scope, member.id)
        return AutoCloseable {
            scope.execute(read, member, channel)
            lease.close()
            present.close()
        }
    }
}