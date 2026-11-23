package boxoffice.web

import boxoffice.domain.Actor
import boxoffice.domain.ShowSeat
import boxoffice.feature.BoxOfficeFeature
import boxoffice.feature.OrdersFeature
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

val asFan = { request: Request -> Actor.Fan(principal(request).id) }

fun BoxOfficeRoutes(boxOffice: BoxOfficeFeature, orders: OrdersFeature, workspace: Workspace): RoutingHttpHandler =
    routes(
        "/shows/{show}/seats/{seat}/reserve" bind POST to boxOffice.reserve(workspace, asFan) { ShowSeat(showId(it), seat(it)) },
        "/shows/{show}/seats/{seat}/release" bind POST to boxOffice.release(workspace, asFan) { ShowSeat(showId(it), seat(it)) },
        "/shows/{show}/checkout" bind POST to orders.checkout(workspace, asFan, input = showId),
    )