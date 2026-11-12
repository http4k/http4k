package boxoffice.store

import boxoffice.domain.Booking
import boxoffice.domain.Order
import boxoffice.domain.OrderId
import boxoffice.domain.Show
import boxoffice.domain.ShowId
import boxoffice.domain.ShowSeat
import boxoffice.domain.Theatre
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke

class BoxOfficeTables(tables: Tables) {
    val shows = tables<ShowId, Show>(SHOWS)
    val bookings = tables<ShowSeat, Booking>(BOOKINGS)
    val orders = tables<OrderId, Order>(ORDERS)
    val theatre = tables<String, Theatre>(THEATRES)

    companion object {
        val SHOWS = TableName.of("shows")
        val BOOKINGS = TableName.of("bookings")
        val ORDERS = TableName.of("orders")
        val THEATRES = TableName.of("theatre")
        val all = listOf(SHOWS, BOOKINGS, ORDERS, THEATRES)
    }
}