package sl4k.web

import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import sl4k.feature.ChatFeature
import sl4k.view.ChatSignals

fun ThreadPage(workspace: Workspace, chat: ChatFeature, regions: ChatRegions, renderer: TemplateRenderer) = Page(
    workspace,
    Layout(regions.channelPage + regions.thread, renderer, subject = regions.thread, signals = ChatSignals()) { request, shell ->
        channelPageView(workspace, chat, request, shell, messageId(request))
    },
    Viewing(onConnect = enterChannel(chat, channelName)),
)