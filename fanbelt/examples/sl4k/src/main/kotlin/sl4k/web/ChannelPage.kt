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
import sl4k.domain.Viewer
import sl4k.feature.ChatFeature
import sl4k.view.ChannelPageView
import sl4k.view.ChatSignals

fun ChannelPage(workspace: Workspace, chat: ChatFeature, regions: ChatRegions, renderer: TemplateRenderer) = Page(
    workspace,
    Layout(regions.channelPage, renderer, subject = regions.history, signals = ChatSignals()) { scope, shell, (me, _, channel) -> channelPageView(chat, scope, shell, me, channel) },
    Viewing(principal, channelName, chat.visibleChannel, noSuchChannel(workspace), chat.enter),
)

val channelActorKey = { request: Request -> ActorKey(principal(request), channelName(request)) }

fun noSuchChannel(workspace: Workspace) = { request: Request -> workspace.uri(request).appendToPath("no-such-channel") }

fun channelPageView(chat: ChatFeature, scope: Scope, shell: PageShell, me: Viewer, channel: Channel, thread: MessageId? = null) =
    ChannelPageView(shell, channel.id, channel.label(me.id), me.id, scope.read(chat.firstUnread, Reader(channel.id, me.id)), thread)
