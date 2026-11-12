package sl4k.web

import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.state.join
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import sl4k.feature.ChatFeature
import sl4k.view.ChatSignals
import sl4k.view.SearchPageView

fun SearchPage(workspace: Workspace, chat: ChatFeature, regions: ChatRegions, renderer: TemplateRenderer) = Page(
    workspace,
    Layout(listOf(regions.channels, regions.people, regions.results), renderer, signals = ChatSignals()) { request, shell ->
        SearchPageView(shell, query(request), principal(request).id)
    },
    Viewing { scope, request -> chat.online.join(scope, principal(request).id) },
)
