package sl4k.web

import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.value
import sl4k.domain.AttachmentId
import sl4k.domain.BookmarkId
import sl4k.domain.ChannelName
import sl4k.domain.MessageId
import sl4k.domain.PAGE
import sl4k.domain.ViewerId

val channelName = Path.value(ChannelName).of("channel")
val viewerId = Path.value(ViewerId).of("viewer")
val messageId = Path.value(MessageId).of("message")
val bookmarkId = Path.value(BookmarkId).of("bookmark")
val attachmentId = Path.value(AttachmentId).of("attachment")
val position = Path.int().of("position")
val emoji = Path.of("emoji")
val handle = Query.optional("handle")
val query = Query.defaulted("q", "")
val show = Query.int().defaulted("show", PAGE)