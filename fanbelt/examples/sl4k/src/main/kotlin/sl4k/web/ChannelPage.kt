package sl4k.web

import org.http4k.core.Request
import org.http4k.core.appendToPath
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import sl4k.domain.Channel
import sl4k.domain.MessageId
import sl4k.domain.Reader
import sl4k.feature.ChatFeature
import sl4k.view.ChannelPageView
import sl4k.view.ChatSignals

fun ChannelPage(workspace: Workspace, chat: ChatFeature, regions: ChatRegions, renderer: TemplateRenderer) = Page(
    workspace,
    Layout(regions.channelPage, renderer, subject = regions.history, signals = ChatSignals()) { request, shell, channel: Channel -> channelPageView(workspace, chat, request, shell, channel) },
    channelViewing(workspace, chat),
)

val channelActorKey = { request: Request -> ActorKey(principal(request), channelName(request)) }

fun channelViewing(workspace: Workspace, chat: ChatFeature) =
    Viewing(channelActorKey, chat.visibleChannel, { workspace.uri(it).appendToPath("no-such-channel") }, enterChannel(chat))

private fun enterChannel(chat: ChatFeature) = { scope: Scope, request: Request, channel: Channel -> chat.enter(scope, principal(request), channel.id) }

fun channelPageView(workspace: Workspace, chat: ChatFeature, request: Request, shell: PageShell, channel: Channel, thread: MessageId? = null): ChannelPageView {
    val scope = workspace.scope(request)
    val me = principal(request)
    return ChannelPageView(shell, channel.id, chat.heading(scope, me, channel.id), me.id, scope.read(chat.firstUnread, Reader(channel.id, me.id)), thread)
}
