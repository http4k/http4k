package helpdesk.web

import helpdesk.domain.Actor
import helpdesk.domain.StatusChange
import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.TicketsFeature
import helpdesk.view.ProviderView
import helpdesk.view.ReplySignals
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.datastar.Element
import org.http4k.datastar.MorphMode.outer
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.datastarElements
import org.http4k.lens.location
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer

enum class ProviderState { Up, Down }

private val agent = { request: Request -> Actor.Agent(principal(request).id) }

fun TicketRoutes(tickets: TicketsFeature, workspace: Workspace, provider: FakeEmailProvider, renderer: TemplateRenderer) = routes(
    "/" bind GET to { request ->
        when (val first = workspace.scope(request).read(tickets) { it.queue().firstOrNull()?.id }) {
            null -> seeOther("agents/${principal(request).id}")
            else -> seeOther("t/$first")
        }
    },
    "/agents" bind GET to { request -> seeOther("agents/${principal(request).id}") },
    "/t/{ticket}/reply" bind POST to tickets.reply(
        workspace, agent, { signals: ReplySignals, request -> ticketId(request) to signals.reply },
        reset = setOf(ReplySignals::reply),
    ),
    "/t/{ticket}/assign" bind POST to tickets.take(workspace, agent, input = ticketId),
    "/t/{ticket}/status/{status}" bind POST to tickets.setStatus(workspace, agent) { StatusChange(ticketId(it), status(it)) },
    "/provider/{state}" bind POST to CatchLensFailure().then { request ->
        provider.set(providerState(request) == ProviderState.Up)
        Response(OK).datastarElements(Element.of(renderer(ProviderView(provider.isUp))), morphMode = outer)
    },
)

private fun seeOther(path: String) = Response(SEE_OTHER).location(Uri.of(path))