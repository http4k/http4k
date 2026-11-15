package helpdesk.web

import helpdesk.domain.Actor
import helpdesk.feature.TicketsFeature
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.tenancy.Workspace

val asAgent = { request: Request -> ActorKey<Actor, Unit>(Actor.Agent(principal(request).id), Unit) }

fun AgentsOnly(workspace: Workspace, tickets: TicketsFeature) = Filter { next ->
    { request -> if (workspace.scope(request).read(tickets.anAgent, asAgent(request)) is Allowed) next(request) else Response(FORBIDDEN) }
}
