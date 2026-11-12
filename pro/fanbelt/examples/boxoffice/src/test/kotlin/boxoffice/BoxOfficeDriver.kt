package boxoffice

import boxoffice.domain.OrderStatus
import boxoffice.domain.SeatRef
import boxoffice.domain.ShowTitle
import org.http4k.fanbelt.testing.Verdict
import java.time.Duration

interface BoxOfficeDriver {
    fun fan(name: String): Fan
    fun tick(by: Duration)
    fun cardsAreDeclined()
}

interface Fan {
    val name: String
    fun watches(show: ShowTitle)
    fun reserved(show: ShowTitle, seat: SeatRef): Verdict
    fun releases(show: ShowTitle, seat: SeatRef): Verdict
    fun checksOut(show: ShowTitle): Verdict
    fun sees(show: ShowTitle): ShowSeen
    fun seesHeader(show: ShowTitle): HeaderSeen
    fun basket(show: ShowTitle): BasketSeen
}

data class ShowSeen(val mine: Set<SeatRef>, val taken: Set<SeatRef>)

data class HeaderSeen(val seatsLeft: Int, val watching: Set<String>)

data class BasketSeen(val reserved: List<SeatRef>, val tickets: List<TicketsSeen>)

data class TicketsSeen(val seats: List<SeatRef>, val status: OrderStatus)
