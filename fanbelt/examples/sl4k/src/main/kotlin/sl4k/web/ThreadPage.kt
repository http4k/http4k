package sl4k.web

import org.http4k.core.Request
import org.http4k.fanbelt.feature.invoke
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import sl4k.domain.MessageRef
import sl4k.feature.ChatFeature
import sl4k.view.ChatSignals

fun ThreadPage(workspace: Workspace, chat: ChatFeature, regions: ChatRegions, renderer: TemplateRenderer) = Page(
    workspace,
    Layout(regions.channelPage + regions.thread, renderer, subject = regions.thread, signals = ChatSignals()) { scope, shell, (me, thread, channel) ->
        channelPageView(chat, scope, shell, me, channel, thread.message)
    },
    Viewing(principal, { request: Request -> MessageRef(channelName(request), messageId(request)) }, chat.visibleThread, noSuchChannel(workspace), chat.enter({ it.actor }) { it.key.channel }),
)
