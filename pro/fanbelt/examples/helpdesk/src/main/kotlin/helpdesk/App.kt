package helpdesk

import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.ActivityFeature
import helpdesk.feature.OrgPolicy
import helpdesk.feature.TicketsFeature
import helpdesk.jobs.HelpdeskJobs
import helpdesk.store.Orgs
import helpdesk.web.AgentsPage
import helpdesk.web.CustomerCardRoutes
import helpdesk.web.EnsureViewer
import helpdesk.web.HomeRoutes
import helpdesk.web.Nav
import helpdesk.web.TicketPage
import helpdesk.web.TicketRoutes
import helpdesk.web.identified
import org.http4k.core.Brotli
import org.http4k.core.ContentEncoding
import org.http4k.core.then
import org.http4k.events.EventFilters
import org.http4k.events.then
import org.http4k.fanbelt.events.ReportFailures
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.page.bind
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.filter.PolyFilters
import org.http4k.routing.poly
import org.http4k.template.HandlebarsTemplates

fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra, provider: FakeEmailProvider, policy: OrgPolicy): Node {
    val runtime = runtime.withEvents(
        EventFilters.AddTimestamp(runtime.clock).then(EventFilters.AddEventName()).then(runtime.events))
    val infra = infrastructure(runtime)
    val tickets = TicketsFeature(runtime.random, policy)
    val activity = ActivityFeature(tickets)
    val orgs = Orgs(infra.storage)
    val workspace = Workspace(infra, "/orgs/{org}", orgs::exists)
    val jobs = HelpdeskJobs(runtime, infra, tickets)
    val renderer = HandlebarsTemplates().CachingClasspath()
    val nav = Nav(tickets, renderer)

    val oneOrg = poly(
        listOf(TicketRoutes(tickets, workspace, provider, renderer), CustomerCardRoutes(workspace, tickets, renderer)),
        "/t/{ticket}" bind TicketPage(workspace, tickets, activity, nav, provider, renderer),
        "/agents/{agent}" bind AgentsPage(workspace, tickets, activity, nav, provider, renderer),
    )

    val home = poly(EnsureViewer(runtime.random).then(HomeRoutes(orgs, policy, workspace, renderer, runtime.clock)))

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, workspace.routes(identified.then(oneOrg))))

    return Node(app, runtime, infra, jobs)
}
