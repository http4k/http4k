package sl4k.web

import org.http4k.core.Request
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import sl4k.domain.ChannelName
import sl4k.domain.MessageId
import sl4k.feature.ChatFeature
import sl4k.view.ChannelPageView
import sl4k.view.ChatSignals

fun ChannelPage(workspace: Workspace, chat: ChatFeature, regions: ChatRegions, renderer: TemplateRenderer) = Page(
    workspace,
    Layout(regions.channelPage, renderer, subject = regions.history, signals = ChatSignals()) { request, shell -> channelPageView(workspace, chat, request, shell) },
    Viewing(onConnect = enterChannel(chat, channelName)),
)

fun enterChannel(chat: ChatFeature, channel: (Request) -> ChannelName) = { scope: Scope, request: Request -> chat.enter(scope, principal(request), channel(request)) }

fun channelPageView(workspace: Workspace, chat: ChatFeature, request: Request, shell: PageShell, thread: MessageId? = null): ChannelPageView {
    val scope = workspace.scope(request)
    val channel = channelName(request)
    return ChannelPageView(shell, channel, chat.heading(scope, principal(request), channel), principal(request).id, chat.firstUnread(scope, principal(request), channel), thread)
}