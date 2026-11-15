package helpdesk.web

import helpdesk.domain.Actor
import helpdesk.feature.TicketsFeature
import helpdesk.view.GateView
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.http.Guard
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

private val asAgent = { request: Request -> ActorKey<Actor, Unit>(Actor.Agent(principal(request).id), Unit) }

fun AgentsOnly(workspace: Workspace, tickets: TicketsFeature) = Guard({ Response(FORBIDDEN) }) { request ->
    request.takeIf { workspace.scope(it).read(tickets.anAgent, asAgent(it)) is Allowed }
}

fun AgentsGate(tickets: TicketsFeature, renderer: TemplateRenderer) =
    Region("gate", renderer, asAgent, tickets.anAgent) { access, _ -> GateView(access is Allowed) }
