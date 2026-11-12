package exchange.web

import exchange.domain.Actor
import exchange.domain.TicketInput
import exchange.feature.MarketFeature
import exchange.view.TicketSignals
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

fun ExchangeRoutes(exchange: MarketFeature, workspace: Workspace): RoutingHttpHandler {
    val trader = { request: Request -> Actor.Trader(principal(request).id) }

    return routes(
        "/s/{symbol}/orders/{side}" bind POST to exchange.place(workspace, trader, { signals: TicketSignals, request ->
            TicketInput(symbolPath(request), sidePath(request), signals.price, signals.quantity)
        }),
        "/orders/{order}/cancel" bind POST to exchange.cancel(workspace, trader) { request -> orderPath(request) },
    )
}