package sl4k.domain

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.length
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.regex
import org.http4k.connect.model.Base64Blob
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.Validated
import org.http4k.fanbelt.feature.Validated.Valid
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.store.Identified
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.UUID

const val PAGE = 50

const val MAX_FILE_MB = 5
const val MAX_FILE_BYTES = MAX_FILE_MB * 1024 * 1024
const val MAX_FILES = 10
const val MAX_TOPIC = 250
const val MAX_CANVAS = 20000

val TYPING_FOR: Duration = Duration.ofSeconds(5)

val textField = FieldName.of("text")

val nameField = FieldName.of("name")

val replyField = FieldName.of("reply")

val reactionField = FieldName.of("reaction")

val editField = FieldName.of("edit")

val topicField = FieldName.of("topic")

val bookmarkField = FieldName.of("addingBookmark")

val pinField = FieldName.of("pin")

val canvasField = FieldName.of("canvas")

val uploadField = FieldName.of("upload")

enum class Emoji(val glyph: String) { thumbs("👍"), tada("🎉"), heart("❤️"), laugh("😂"), eyes("👀") }

data class ReactionId(val message: MessageId, val emoji: Emoji, val member: ViewerId)

data class Reaction(override val id: ReactionId, val channel: ChannelName) : Identified<ReactionId>

data class React(val channel: ChannelName, val message: MessageId, val emoji: String)

typealias Reactions = Map<MessageId, Map<Emoji, Set<ViewerId>>>

class ChannelName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ChannelName>(::ChannelName, "[a-z0-9][a-z0-9-]{0,39}".regex)
}

val GENERAL = ChannelName.of("general")

class ChannelTopic private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<ChannelTopic, String>(::ChannelTopic, (0..MAX_TOPIC).length, String::trim)
}

class BookmarkId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<BookmarkId>(::BookmarkId)
}

class BookmarkTitle private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<BookmarkTitle, String>(::BookmarkTitle, (1..80).length.and { it.isNotBlank() }, String::trim)
}

class BookmarkUrl private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<BookmarkUrl, String>(::BookmarkUrl, (1..2000).length.and(::isWebLink), String::trim)
}

const val MAX_PREVIEWS = 3

const val webLink = """https?://\S*[^\s.,;:!?)\]]"""

const val codeSpan = """```(.+?)```|`([^`]+)`"""

private val code = Regex(codeSpan)

fun linksIn(text: String) = Regex(webLink).findAll(text.replace(code, " ")).map { it.value }.filter(::isWebLink).distinct().take(MAX_PREVIEWS).toList()

fun isWebLink(text: String): Boolean {
    val link = runCatching { URI(text) }.getOrNull() ?: return false
    return link.scheme?.lowercase() in setOf("http", "https") && !link.host.isNullOrBlank()
}

data class Bookmark(override val id: BookmarkId, val channel: ChannelName, val title: BookmarkTitle, val url: BookmarkUrl) : Identified<BookmarkId>

class AttachmentId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<AttachmentId>(::AttachmentId)
}

class AttachmentName private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<AttachmentName, String>(::AttachmentName, (1..255).length.and { it.isNotBlank() }, String::trim)
}

data class Attachment(override val id: AttachmentId, val message: MessageId, val channel: ChannelName, val name: AttachmentName, val type: String) : Identified<AttachmentId> {
    val inline get() = type in inlineImages
    val servedAs get() = if (inline) type else "application/octet-stream"
    val safeName get() = name.value.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

data class AttachmentContent(override val id: AttachmentId, val contents: Base64Blob) : Identified<AttachmentId>

data class Upload(val name: String = "", val mime: String = "", val contents: String = "")

data class ValidFile(val name: AttachmentName, val mime: String, val contents: Base64Blob)

data class Download(val attachment: Attachment, val contents: Base64Blob)

data class AttachmentRef(val channel: ChannelName, val attachment: AttachmentId)

val inlineImages = setOf("image/png", "image/jpeg", "image/gif", "image/webp")

class CanvasText private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<CanvasText>(::CanvasText, MAX_CANVAS.maxLength)
}

data class Canvas(override val id: ChannelName, val text: CanvasText = CanvasText.of(""), val version: Int = 0) : Identified<ChannelName>

class MessageId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<MessageId>(::MessageId)
}

class MessageText private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<MessageText, String>(::MessageText, (1..4000).length.and { it.isNotBlank() }, String::trim)
}

data class Channel(override val id: ChannelName, val createdAt: Instant, val members: Set<ViewerId>? = null, val direct: Boolean = false, val owner: ViewerId? = null, val topic: ChannelTopic? = null) : Identified<ChannelName> {
    fun visibleTo(member: ViewerId) = members == null || member in members

    fun peerOf(member: ViewerId) = if (direct) members.orEmpty().first { it != member } else null

    fun label(member: ViewerId) = peerOf(member)?.let { Viewer(it).name } ?: "#$id"
}


fun directChannel(one: ViewerId, other: ViewerId) = ChannelName.of("dm-" + listOf(one, other).map { it.value.toString().take(8) }.sorted().joinToString("-"))

data class Message(override val id: MessageId, val channel: ChannelName, val author: ViewerId, val text: MessageText, val at: Instant, val mentions: Set<ViewerId> = emptySet(), val thread: MessageId? = null, val editedAt: Instant? = null, val deletedAt: Instant? = null, val pinnedAt: Instant? = null) : Identified<MessageId> {
    val deleted get() = deletedAt != null
}

val mention = Regex("@([a-z]+-[a-z]+)")

private val typingHandle = Regex("@([a-z-]*)$")

fun handleBeingTyped(text: String) = typingHandle.find(text)?.groupValues?.get(1)

fun completed(text: String, handle: String) = text.replace(typingHandle, "@$handle ")

data class Reader(val channel: ChannelName, val member: ViewerId)

data class ReadMark(override val id: Reader, val count: Int) : Identified<Reader>

data class Team(val createdAt: Instant, override val id: String = SELF) : Identified<String> {
    companion object {
        const val SELF = "self"
    }
}

enum class PreviewState { Pending, Ready }

data class PreviewId(val message: MessageId, val position: Int)

data class Preview(override val id: PreviewId, val channel: ChannelName, val url: String, val state: PreviewState = PreviewState.Pending, val title: String = "", val description: String = "", val image: String = "") : Identified<PreviewId>

data class Unfurled(val preview: PreviewId, val url: String, val title: String, val description: String, val image: String = "")

data class Unreadable(val preview: PreviewId, val url: String)

data object Unfurler

private fun validFile(upload: Upload): Validated<ValidFile> {
    val name = uploadField.check(AttachmentName, upload.name, "Give the file a name").orElse { return it }
    val contents = uploadField.check(Base64Blob, upload.contents, "That file can't be read").orElse { return it }
    val size = runCatching { contents.decodedBytes().size }.getOrNull() ?: return Rejected(uploadField, "That file can't be read")
    if (size > MAX_FILE_BYTES) return Rejected(uploadField, "Keep files to $MAX_FILE_MB MB")
    return Valid(ValidFile(name, upload.mime, contents))
}

data class PreviewRef(val channel: ChannelName, val preview: PreviewId)

data class ChannelMessage(val channel: Channel, val message: Message)

data class Draft(val channel: ChannelName, val text: String, val uploads: List<Upload> = emptyList()) {
    val files = uploads.map(::validFile)
}

const val MAX_RESULTS = 50

data class SearchKey(val member: ViewerId, val query: String)

data class Hit(val message: Message, val label: String)

data class Window(val channel: ChannelName, val show: Int)

data class Typing(val until: Instant)

data class Roster(val teammates: List<ViewerId>, val here: Set<ViewerId>)

data class Membership(val private: Boolean, val owner: ViewerId?, val members: Set<ViewerId>, val invitable: List<ViewerId>)

data class NewBookmark(val channel: ChannelName, val title: String, val url: String)

data class BookmarkRef(val channel: ChannelName, val bookmark: BookmarkId)

data class CanvasEdit(val channel: ChannelName, val text: String, val basedOn: Int)

data class TopicChange(val channel: ChannelName, val text: String)

data class NewChannel(val name: String, val private: Boolean)

data class SidebarChannel(val name: ChannelName, val unread: Int, val with: ViewerId?, val private: Boolean, val mentions: Int)

data class Sidebars(private val members: Map<ViewerId, List<SidebarChannel>>, private val stranger: List<SidebarChannel>) {
    fun of(member: ViewerId) = members[member] ?: stranger
}

data class Reply(val channel: ChannelName, val parent: MessageId, val text: String)

data class MessageRef(val channel: ChannelName, val message: MessageId)

data class Edit(val channel: ChannelName, val message: MessageId, val text: String)

data class Unread(val messages: Int, val mentions: Int)

data class ThreadKey(val channel: ChannelName, val parent: MessageId)

data class ReplySummary(val count: Int, val last: Instant)

data class Thread(val parent: Message, val replies: List<Message>, val reactions: Reactions)

data class History(val channel: ChannelName, val messages: List<Message>, val older: Boolean, val replies: Map<MessageId, ReplySummary>, val reactions: Reactions, val attachments: Map<MessageId, List<Attachment>>, val previews: Map<MessageId, List<Preview>>)

