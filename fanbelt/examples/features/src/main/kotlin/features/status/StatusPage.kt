package features.status

import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

data class StatusView(val message: String) : ViewModel

data class StatusPageView(val shell: PageShell) : ViewModel

fun StatusPage(workspace: Workspace, status: StatusFeature, renderer: TemplateRenderer): Page {
    val current = Region("status", renderer, status.current) { it -> StatusView(it.message) }
    return Page(workspace, Layout(listOf(current), renderer) { shell -> StatusPageView(shell) })
}
