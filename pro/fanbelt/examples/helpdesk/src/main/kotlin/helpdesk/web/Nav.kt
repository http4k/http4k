package helpdesk.web

import helpdesk.domain.Viewer
import helpdesk.feature.TicketsFeature
import helpdesk.view.NavView
import helpdesk.view.OnlineAgent
import org.http4k.fanbelt.page.Region
import org.http4k.routing.path
import org.http4k.template.TemplateRenderer

fun Nav(tickets: TicketsFeature, renderer: TemplateRenderer) = Region("nav", renderer, tickets.nav) { model, request ->
    val me = principal(request).id
    NavView(
        onAgents = request.path("agent") != null,
        open = model.counts.open,
        pending = model.counts.pending,
        resolved = model.counts.resolved,
        mine = model.open[me] ?: 0,
        online = model.online.map { OnlineAgent(it, Viewer(it).name, it == me) }.sortedBy { it.name },
    )
}