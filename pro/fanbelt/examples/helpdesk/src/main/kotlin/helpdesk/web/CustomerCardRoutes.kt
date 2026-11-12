package helpdesk.web

import helpdesk.feature.TicketsFeature
import helpdesk.view.CustomerCardGoneView
import helpdesk.view.CustomerCardView
import helpdesk.view.CustomerTicket
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.datastar.Element
import org.http4k.datastar.MorphMode.outer
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.datastarElements
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer

fun CustomerCardRoutes(workspace: Workspace, tickets: TicketsFeature, renderer: TemplateRenderer): RoutingHttpHandler = routes(
    "/t/{ticket}/customer" bind GET to CatchLensFailure().then { request ->
        val card = workspace.scope(request).read(tickets.customer, ticketId(request))
        val view = if (card == null) CustomerCardGoneView
        else CustomerCardView(card.customer, card.tickets.map { CustomerTicket(it.number, it.subject, it.status) })
        Response(OK).datastarElements(Element.of(renderer(view)), morphMode = outer)
    },
    "/customer-card/close" bind GET to {
        Response(OK).datastarElements(Element.of("""<div id="customer-card"></div>"""), morphMode = outer)
    },
)
