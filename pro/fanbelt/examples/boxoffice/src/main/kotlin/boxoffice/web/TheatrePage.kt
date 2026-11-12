package boxoffice.web

import boxoffice.feature.BoxOfficeFeature
import boxoffice.view.ShowsView
import boxoffice.view.TheatrePageView
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

fun TheatrePage(workspace: Workspace, boxOffice: BoxOfficeFeature, renderer: TemplateRenderer): Page {
    val shows = Region("shows", renderer, boxOffice.shows) { shows, _ -> ShowsView(shows) }

    return Page(workspace, Layout(listOf(shows), renderer) { _, shell -> TheatrePageView(shell) })
}