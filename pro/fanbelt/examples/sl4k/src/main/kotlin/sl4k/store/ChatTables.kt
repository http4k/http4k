package sl4k.store

import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke
import sl4k.domain.Attachment
import sl4k.domain.AttachmentContent
import sl4k.domain.AttachmentId
import sl4k.domain.Bookmark
import sl4k.domain.BookmarkId
import sl4k.domain.Canvas
import sl4k.domain.Channel
import sl4k.domain.ChannelName
import sl4k.domain.Message
import sl4k.domain.MessageId
import sl4k.domain.Preview
import sl4k.domain.PreviewId
import sl4k.domain.Reaction
import sl4k.domain.ReactionId
import sl4k.domain.ReadMark
import sl4k.domain.Reader
import sl4k.domain.Team
import sl4k.domain.ViewerId

class ChatTables(tables: Tables) {
    val channels = tables<ChannelName, Channel>(CHANNELS)
    val messages = tables<MessageId, Message>(MESSAGES)
    val marks = tables<Reader, ReadMark>(MARKS)
    val reactions = tables<ReactionId, Reaction>(REACTIONS)
    val team = tables<String, Team>(TEAMS)
    val bookmarks = tables<BookmarkId, Bookmark>(BOOKMARKS)
    val canvases = tables<ChannelName, Canvas>(CANVASES)
    val attachments = tables<AttachmentId, Attachment>(ATTACHMENTS)
    val attachmentContents = tables<AttachmentId, AttachmentContent>(ATTACHMENT_CONTENTS)
    val previews = tables<PreviewId, Preview>(PREVIEWS)

    companion object {
        val CHANNELS = TableName.of("channels")
        val MESSAGES = TableName.of("messages")
        val MARKS = TableName.of("marks")
        val REACTIONS = TableName.of("reactions")
        val TEAMS = TableName.of("team")
        val BOOKMARKS = TableName.of("bookmarks")
        val CANVASES = TableName.of("canvases")
        val ATTACHMENTS = TableName.of("attachments")
        val ATTACHMENT_CONTENTS = TableName.of("attachment_contents")
        val PREVIEWS = TableName.of("previews")
    }
}