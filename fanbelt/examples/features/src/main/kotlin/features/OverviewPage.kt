package features

import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

data class OverviewPageView(val shell: PageShell) : ViewModel

fun OverviewPage(workspace: Workspace, renderer: TemplateRenderer) =
    Page(workspace, Layout(emptyList(), renderer) { shell -> OverviewPageView(shell) })
