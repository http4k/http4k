package boxoffice.view

import boxoffice.domain.OrderStatus
import boxoffice.domain.SeatRef
import boxoffice.domain.Show
import boxoffice.domain.ShowId
import boxoffice.domain.ShowTitle
import boxoffice.domain.ViewerId
import org.http4k.fanbelt.page.PageShell
import org.http4k.template.ViewModel

data class ShowSignals(val seat: String = "")

data class ShowsView(val shows: List<Show>) : ViewModel

data class SeatButton(val seat: SeatRef, val number: Int, val reservedBy: String)

data class SeatRow(val label: Char, val seats: List<SeatButton>)

data class SeatMapView(val show: ShowId, val rows: List<SeatRow>) : ViewModel

data object ShowGoneView : ViewModel

data class HeaderView(val title: ShowTitle, val left: Int, val watching: List<String>) : ViewModel

data object HeaderGoneView : ViewModel

data class TicketRow(val seats: String, val status: OrderStatus)

data class BasketView(val show: ShowId, val reserved: List<SeatRef>, val tickets: List<TicketRow>) : ViewModel

data class TheatrePageView(val shell: PageShell) : ViewModel

data class ShowPageView(val shell: PageShell, val show: ShowId, val viewer: ViewerId) : ViewModel