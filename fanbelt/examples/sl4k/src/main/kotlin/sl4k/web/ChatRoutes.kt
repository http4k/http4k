package sl4k.web

import java.nio.ByteBuffer
import java.time.Duration
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.appendToPath
import org.http4k.core.maxAge
import org.http4k.core.private
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.Element
import org.http4k.fanbelt.input.GoTo
import org.http4k.fanbelt.page.bind
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.format.Moshi
import org.http4k.format.asDatastarSignal
import org.http4k.lens.contentType
import org.http4k.lens.datastarElements
import org.http4k.lens.datastarEvents
import org.http4k.lens.datastarSignals
import org.http4k.lens.html
import org.http4k.lens.location
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import sl4k.domain.AttachmentRef
import sl4k.domain.BookmarkRef
import sl4k.domain.CanvasEdit
import sl4k.domain.ChannelName
import sl4k.domain.Completing
import sl4k.domain.Draft
import sl4k.domain.Edit
import sl4k.domain.GENERAL
import sl4k.domain.MessageRef
import sl4k.domain.NewBookmark
import sl4k.domain.NewChannel
import sl4k.domain.PreviewRef
import sl4k.domain.React
import sl4k.domain.Reader
import sl4k.domain.Reply
import sl4k.domain.Seen
import sl4k.domain.TopicChange
import sl4k.domain.Typed
import sl4k.domain.Viewer
import sl4k.domain.inlineImages
import sl4k.feature.ChatFeature
import sl4k.view.ChatSignals
import sl4k.view.NoSuchChannelView
import sl4k.view.SuggestView

private fun Response.lockedDown() = header("x-content-type-options", "nosniff").header("content-security-policy", "sandbox")

fun ChatRoutes(chat: ChatFeature, workspace: Workspace, renderer: TemplateRenderer, sites: HttpHandler): PolyHandler {
    val regions = ChatRegions(chat, renderer)
    val signals = { request: Request -> Moshi.asA<ChatSignals>(request.bodyString()) }
    val messageRef = { request: Request -> MessageRef(channelName(request), messageId(request)) }
    val teammate = { request: Request -> Reader(channelName(request), viewerId(request)) }
    val general = { request: Request -> workspace.uri(request).appendToPath("c/$GENERAL") }
    val toChannel = GoTo<ChannelName> { name -> Uri.of("c/$name") }
    val typed = { request: Request -> Typed(channelName(request), signals(request).text) }

    val requests = routes(
        "/" bind GET to { request -> Response(SEE_OTHER).location(general(request)) },
        "/no-such-channel" bind GET to { request -> Response(NOT_FOUND).html(renderer(NoSuchChannelView(general(request)))) },
        "/c/{channel}" bind routes(
            "/messages" bind POST to workspace.execute(chat.post, principal, channelName, reset = setOf(ChatSignals::text, ChatSignals::_files)) { signals: ChatSignals, channel -> Draft(channel, signals.text, signals.upload) },
            "/t/{message}/replies" bind POST to workspace.execute(chat.reply, principal, messageRef, reset = setOf(ChatSignals::reply)) { signals: ChatSignals, ref -> Reply(ref.channel, ref.message, signals.reply) },
            "/m/{message}/edit" bind POST to workspace.execute(chat.edit, principal, messageRef, reset = setOf(ChatSignals::edit, ChatSignals::editing)) { signals: ChatSignals, ref -> Edit(ref.channel, ref.message, signals.edit) },
            "/m/{message}/delete" bind POST to workspace.execute(chat.delete, principal, input = messageRef),
            "/m/{message}/pin" bind POST to workspace.execute(chat.togglePin, principal, input = messageRef),
            "/m/{message}/react/{emoji}" bind POST to workspace.execute(chat.react, principal) { React(channelName(it), messageId(it), emoji(it)) },
            "/topic" bind POST to workspace.execute(chat.setTopic, principal, channelName, reset = setOf(ChatSignals::topic, ChatSignals::editingTopic)) { signals: ChatSignals, channel -> TopicChange(channel, signals.topic) },
            "/bookmarks" bind POST to workspace.execute(chat.addBookmark, principal, channelName, reset = setOf(ChatSignals::title, ChatSignals::url, ChatSignals::addingBookmark)) { signals: ChatSignals, channel -> NewBookmark(channel, signals.title, signals.url) },
            "/bookmarks/{bookmark}/remove" bind POST to workspace.execute(chat.removeBookmark, principal) { BookmarkRef(channelName(it), bookmarkId(it)) },
            "/canvas" bind POST to workspace.execute(chat.saveCanvas, principal, channelName, reset = setOf(ChatSignals::canvas, ChatSignals::editingCanvas)) { signals: ChatSignals, channel -> CanvasEdit(channel, signals.canvas, signals.canvasBase) },
            "/read/{seen}" bind POST to workspace.execute(chat.read, principal) { Seen(channelName(it), seen(it)) },
            "/leave" bind POST to workspace.execute(chat.leave, principal, feedback = GoTo { Uri.of("c/$GENERAL") }, input = channelName),
            "/remove/{viewer}" bind POST to workspace.execute(chat.remove, principal, input = teammate),
            "/invite/{viewer}" bind POST to workspace.execute(chat.invite, principal, input = teammate),
            "/files/{attachment}" bind GET to workspace.read(chat.visibleFile, principal, { AttachmentRef(channelName(it), attachmentId(it)) }) { found, _ ->
                Response(OK)
                    .header("content-type", found.attachment.servedAs)
                    .header("content-disposition", (if (found.attachment.inline) "inline" else "attachment") + "; filename=\"${found.attachment.safeName}\"")
                        .lockedDown()
                    .private()
                    .body(Body(ByteBuffer.wrap(found.contents.decodedBytes())))
            },
            "/previews/{message}/{position}/image" bind GET to workspace.read(chat.visibleImage, principal, { PreviewRef(channelName(it), messageId(it), position(it)) }) { image, _ ->
                val fetched = sites(Request(GET, image))
                val type = fetched.contentType()?.value?.lowercase()
                if (!fetched.status.successful || type !in inlineImages) Response(NOT_FOUND)
                else Response(OK).header("content-type", type).body(fetched.body).lockedDown().private().maxAge(Duration.ofDays(1))
            },
        ),
        "/c/{channel}" bind routes(
            "/typing" bind POST to workspace.carry(chat.here, { principal(it).id }, channelName) { chat.typing() },
            "/suggest" bind POST to workspace.read(chat.mentions, principal, typed) { found, typed ->
                Response(OK).datastarElements(renderer(SuggestView(typed.channel, found.take(5).map(::Viewer))))
            },
            "/complete" bind POST to workspace.read(chat.completion, principal, { Completing(typed(it), handle(it)) }) { done, completing ->
                Response(OK).datastarEvents(listOf(
                    DatastarEvent.PatchSignals(Moshi.asDatastarSignal(mapOf("text" to done))),
                    DatastarEvent.PatchElements(listOf(Element.of(renderer(SuggestView(completing.typed.channel, emptyList()))))),
                ))
            },
            "/m/{message}/editing" bind POST to workspace.read(chat.yourMessage, principal, messageRef) { found, ref ->
                Response(OK).datastarSignals(Moshi.asDatastarSignal(mapOf("editing" to ref.message.toString(), "edit" to found.message.text.value)))
            },
            "/topic/editing" bind POST to workspace.read(chat.visibleChannel, principal, channelName) { channel, _ ->
                Response(OK).datastarSignals(Moshi.asDatastarSignal(mapOf("editingTopic" to true, "topic" to channel.topic?.value.orEmpty())))
            },
            "/canvas/editing" bind POST to workspace.read(chat.visibleCanvas, principal, channelName) { canvas, _ ->
                Response(OK).datastarSignals(Moshi.asDatastarSignal(mapOf("editingCanvas" to true, "canvas" to canvas.text.value, "canvasBase" to canvas.version)))
            },
        ),
        "/channels" bind POST to workspace.execute(chat.create, principal, reset = setOf(ChatSignals::name, ChatSignals::private), feedback = toChannel) { signals: ChatSignals -> NewChannel(signals.name, signals.private) },
        "/dm/{viewer}" bind POST to workspace.execute(chat.direct, principal, feedback = toChannel, input = viewerId),
    )

    return poly(
        listOf(requests),
        "/search" bind SearchPage(workspace, chat, regions, renderer),
        "/c/{channel}" bind ChannelPage(workspace, chat, regions, renderer),
        "/c/{channel}/t/{message}" bind ThreadPage(workspace, chat, regions, renderer),
    )
}
