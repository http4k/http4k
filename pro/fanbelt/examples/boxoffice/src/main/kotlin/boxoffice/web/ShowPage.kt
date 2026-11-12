package boxoffice.web

import boxoffice.domain.Shopper
import boxoffice.domain.Viewer
import boxoffice.feature.BoxOfficeFeature
import boxoffice.feature.OrdersFeature
import boxoffice.view.BasketView
import boxoffice.view.HeaderGoneView
import boxoffice.view.HeaderView
import boxoffice.view.SeatButton
import boxoffice.view.SeatMapView
import boxoffice.view.SeatRow
import boxoffice.view.ShowGoneView
import boxoffice.view.ShowPageView
import boxoffice.view.ShowSignals
import boxoffice.view.TicketRow
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

fun ShowPage(workspace: Workspace, boxOffice: BoxOfficeFeature, orders: OrdersFeature, renderer: TemplateRenderer): Page {
    val header = Region("header", renderer, showId, boxOffice.header, fallback = HeaderGoneView) { header, _ ->
        if (header == null) return@Region HeaderGoneView
        HeaderView(header.title, header.seatsLeft, header.watching.map { Viewer(it).name }.sorted())
    }
    val seats = Region("seats", renderer, showId, boxOffice.seatMap, fallback = ShowGoneView) { map, _ ->
        if (map == null) return@Region ShowGoneView
        SeatMapView(map.show.id, map.seats.groupBy { it.seat.row }.map { (row, seats) ->
            SeatRow(row, seats.map { SeatButton(it.seat, it.seat.number, it.reservedBy?.toString().orEmpty()) })
        })
    }
    val basket = Region("basket", renderer, { request -> Shopper(showId(request), principal(request).id) }, orders.basket) { basket, request ->
        BasketView(showId(request), basket.reserved, basket.tickets.map { TicketRow(it.seats.joinToString(" "), it.status) })
    }

    val layout = Layout(listOf(header, seats, basket), renderer, subject = seats, signals = ShowSignals()) { request, shell ->
        ShowPageView(shell, showId(request), principal(request).id)
    }
    return Page(workspace, layout, Viewing(asFan) { scope, request -> boxOffice.watching.join(scope, showId(request), principal(request).id) })
}