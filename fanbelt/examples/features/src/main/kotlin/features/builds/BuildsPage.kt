package features.builds

import features.FeaturesJson.json
import org.http4k.core.Method.POST
import org.http4k.core.then
import org.http4k.fanbelt.input.Statuses
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

data class BuildRow(val status: String, val commit: String)

data class BuildsView(val builds: List<BuildRow>) : ViewModel

data class BuildsPageView(val shell: PageShell) : ViewModel

fun BuildsPage(workspace: Workspace, builds: BuildsFeature, renderer: TemplateRenderer): Page {
    val latest = Region("builds", renderer, builds.latest) { history -> BuildsView(history.map { BuildRow(it.status.name, it.commit.value) }) }
    return Page(workspace, Layout(listOf(latest), renderer) { shell -> BuildsPageView(shell) })
}

fun BuildsWebhook(workspace: Workspace, builds: BuildsFeature, ciToken: String) = routes(
    "/builds" bind POST to CatchLensFailure().then(ServerFilters.BearerAuth(ciToken)).then(
        builds.report(workspace, { Ci }, Statuses()) { it.json<BuildPosted>() }
    ),
)
