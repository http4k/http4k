package sl4k.web

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.appendToPath
import org.http4k.core.maxAge
import org.http4k.core.private
import org.http4k.core.then
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.Element
import org.http4k.format.Moshi
import org.http4k.format.asDatastarSignal
import org.http4k.lens.datastarElements
import org.http4k.lens.contentType
import org.http4k.lens.datastarEvents
import org.http4k.lens.datastarSignals
import org.http4k.lens.location
import org.http4k.fanbelt.input.GoTo
import org.http4k.fanbelt.page.bind
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import sl4k.domain.AttachmentRef
import sl4k.domain.GENERAL
import sl4k.domain.BookmarkRef
import sl4k.domain.ChannelName
import sl4k.domain.CanvasEdit
import sl4k.domain.Draft
import sl4k.domain.Edit
import sl4k.domain.MessageRef
import sl4k.domain.NewBookmark
import sl4k.domain.NewChannel
import sl4k.domain.PreviewId
import sl4k.domain.React
import sl4k.domain.Reader
import sl4k.domain.Reply
import sl4k.domain.TopicChange
import sl4k.domain.Viewer
import sl4k.domain.completed
import sl4k.domain.inlineImages
import sl4k.feature.ChatFeature
import sl4k.view.ChatSignals
import sl4k.view.SuggestView
import java.nio.ByteBuffer
import java.time.Duration

private fun Response.lockedDown() = header("x-content-type-options", "nosniff").header("content-security-policy", "sandbox")

fun ChatRoutes(chat: ChatFeature, workspace: Workspace, renderer: TemplateRenderer, sites: HttpHandler): PolyHandler {
    val regions = ChatRegions(chat, renderer)
    val membersOnly = ChannelMembers(workspace, chat)
    val signals = { request: Request -> Moshi.asA<ChatSignals>(request.bodyString()) }
    val messageRef = { request: Request -> MessageRef(channelName(request), messageId(request)) }
    val teammate = { request: Request -> Reader(channelName(request), viewerId(request)) }
    val toChannel = GoTo<ChannelName> { name -> Uri.of("c/$name") }
    fun suggestions(request: Request, text: String) = SuggestView(
        channelName(request),
        chat.suggestions(workspace.scope(request), principal(request), channelName(request), text).take(5).map(::Viewer),
    )

    val requests = routes(
        "/" bind GET to { request -> Response(SEE_OTHER).location(workspace.uri(request).appendToPath("c/$GENERAL")) },
        "/c/{channel}" bind routes(
            "/messages" bind POST to chat.post(workspace, principal, { signals: ChatSignals, request -> Draft(channelName(request), signals.text, signals.upload) }, reset = setOf(ChatSignals::text, ChatSignals::_files)),
            "/t/{message}/replies" bind POST to chat.reply(workspace, principal, { signals: ChatSignals, request -> Reply(channelName(request), messageId(request), signals.reply) }, reset = setOf(ChatSignals::reply)),
            "/m/{message}/edit" bind POST to chat.edit(workspace, principal, { signals: ChatSignals, request -> Edit(channelName(request), messageId(request), signals.edit) }, reset = setOf(ChatSignals::edit, ChatSignals::editing)),
            "/m/{message}/delete" bind POST to chat.delete(workspace, principal, input = messageRef),
            "/m/{message}/pin" bind POST to chat.togglePin(workspace, principal, input = messageRef),
            "/m/{message}/react/{emoji}" bind POST to chat.react(workspace, principal) { React(channelName(it), messageId(it), emoji(it)) },
            "/topic" bind POST to chat.setTopic(workspace, principal, { signals: ChatSignals, request -> TopicChange(channelName(request), signals.topic) }, reset = setOf(ChatSignals::topic, ChatSignals::editingTopic)),
            "/bookmarks" bind POST to chat.addBookmark(workspace, principal, { signals: ChatSignals, request -> NewBookmark(channelName(request), signals.title, signals.url) }, reset = setOf(ChatSignals::title, ChatSignals::url, ChatSignals::addingBookmark)),
            "/bookmarks/{bookmark}/remove" bind POST to chat.removeBookmark(workspace, principal) { BookmarkRef(channelName(it), bookmarkId(it)) },
            "/canvas" bind POST to chat.saveCanvas(workspace, principal, { signals: ChatSignals, request -> CanvasEdit(channelName(request), signals.canvas, signals.canvasBase) }, reset = setOf(ChatSignals::canvas, ChatSignals::editingCanvas)),
            "/leave" bind POST to chat.leave(workspace, principal, feedback = GoTo { Uri.of("c/$GENERAL") }, input = channelName),
            "/remove/{viewer}" bind POST to chat.remove(workspace, principal, input = teammate),
            "/invite/{viewer}" bind POST to chat.invite(workspace, principal, input = teammate),
            "/files/{attachment}" bind GET to route@{ request ->
                val found = chat.download(workspace.scope(request), principal(request), AttachmentRef(channelName(request), attachmentId(request))) ?: return@route Response(NOT_FOUND)
                Response(OK)
                    .header("content-type", found.attachment.servedAs)
                    .header("content-disposition", (if (found.attachment.inline) "inline" else "attachment") + "; filename=\"${found.attachment.safeName}\"")
                        .lockedDown()
                    .private()
                    .body(Body(ByteBuffer.wrap(found.contents.decodedBytes())))
            },
            "/previews/{message}/{position}/image" bind GET to route@{ request ->
                val image = chat.previewImage(workspace.scope(request), principal(request), channelName(request), PreviewId(messageId(request), position(request))) ?: return@route Response(NOT_FOUND)
                val fetched = sites(Request(GET, image))
                val type = fetched.contentType()?.value?.lowercase()
                if (!fetched.status.successful || type !in inlineImages) Response(NOT_FOUND)
                else Response(OK).header("content-type", type).body(fetched.body).lockedDown().private().maxAge(Duration.ofDays(1))
            },
        ),
        "/c/{channel}" bind membersOnly.requests.then(routes(
            "/typing" bind POST to chat.here.set(workspace, { principal(it).id }, channelName) { chat.typing() },
            "/suggest" bind POST to { request ->
                Response(OK).datastarElements(renderer(suggestions(request, signals(request).text)))
            },
            "/complete" bind POST to { request ->
                val typed = signals(request).text
                val chosen = handle(request)
                val done = if (chosen == null) chat.complete(workspace.scope(request), principal(request), channelName(request), typed) else completed(typed, chosen)
                Response(OK).datastarEvents(listOf(
                    DatastarEvent.PatchSignals(Moshi.asDatastarSignal(mapOf("text" to done))),
                    DatastarEvent.PatchElements(listOf(Element.of(renderer(suggestions(request, ""))))),
                ))
            },
            "/m/{message}/editing" bind POST to route@{ request ->
                val text = chat.draft(workspace.scope(request), principal(request), messageRef(request)) ?: return@route Response(FORBIDDEN)
                Response(OK).datastarSignals(Moshi.asDatastarSignal(mapOf("editing" to messageId(request).toString(), "edit" to text)))
            },
            "/topic/editing" bind POST to { request ->
                Response(OK).datastarSignals(Moshi.asDatastarSignal(mapOf("editingTopic" to true, "topic" to workspace.scope(request).read(chat.channelTopic, channelName(request)))))
            },
            "/canvas/editing" bind POST to { request ->
                val canvas = workspace.scope(request).read(chat.canvas, channelName(request))
                Response(OK).datastarSignals(Moshi.asDatastarSignal(mapOf("editingCanvas" to true, "canvas" to canvas.text.value, "canvasBase" to canvas.version)))
            },
        )),
        "/channels" bind POST to chat.create(workspace, principal, { signals: ChatSignals, _ -> NewChannel(signals.name, signals.private) }, reset = setOf(ChatSignals::name, ChatSignals::private), feedback = toChannel),
        "/dm/{viewer}" bind POST to chat.direct(workspace, principal, feedback = toChannel, input = viewerId),
    )

    return poly(
        listOf(requests),
        "/search" bind SearchPage(workspace, chat, regions, renderer),
        "/c/{channel}" bind membersOnly.pages.then(poly("" bind ChannelPage(workspace, chat, regions, renderer))),
        "/c/{channel}/t/{message}" bind membersOnly.pages.then(poly("" bind ThreadPage(workspace, chat, regions, renderer))),
    )
}