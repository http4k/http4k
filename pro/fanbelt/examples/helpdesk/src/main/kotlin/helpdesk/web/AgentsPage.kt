package helpdesk.web

import helpdesk.domain.AgentPage
import helpdesk.domain.Viewer
import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.ActivityFeature
import helpdesk.feature.TicketsFeature
import helpdesk.view.AgentFact
import helpdesk.view.AgentRow
import helpdesk.view.AgentTicket
import helpdesk.view.AgentView
import helpdesk.view.AgentsPageView
import helpdesk.view.AgentsView
import helpdesk.view.ProviderView
import helpdesk.view.describe
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.state.join
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

fun AgentsPage(workspace: Workspace, tickets: TicketsFeature, activity: ActivityFeature, nav: Region<*, *>, provider: FakeEmailProvider, renderer: TemplateRenderer): Page {
    val agents = agents(tickets, renderer)
    val agent = agent(activity, renderer)

    return Page(
        workspace = workspace,
        layout = Layout(
            regions = listOf(nav, agents, agent),
            renderer = renderer,
        ) { request, shell -> AgentsPageView(shell, principal(request).name, ProviderView(provider.isUp)) },
        viewing = Viewing { scope, request -> tickets.online.join(scope, principal(request).id) },
    )
}

private fun agents(tickets: TicketsFeature, renderer: TemplateRenderer) = Region(
    "agents", renderer,
    tickets.agents,
) { model, request ->
    val selected = agentId(request)
    AgentsView((model.known + model.online + selected).map { id ->
        AgentRow(id, Viewer(id).name, id in model.online, model.open[id] ?: 0, id == selected)
    }.sortedBy { it.name })
}

private fun agent(activity: ActivityFeature, renderer: TemplateRenderer) = Region(
    "agent", renderer, { AgentPage(agentId(it), before(it)) },
    activity.agentPage,
) { model, request ->
    val id = agentId(request)
    AgentView(
        id,
        Viewer(id).name,
        id == principal(request).id,
        model.assigned.map { AgentTicket(it.id, it.number, it.subject, it.status) },
        model.facts.map { AgentFact(it.ticket, model.numbers[it.ticket] ?: 0, it.describe()) },
        model.facts.next,
        before(request) != null,
    )
}