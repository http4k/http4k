package boxoffice.feature

import boxoffice.domain.Booking
import boxoffice.domain.RESERVED_FOR
import boxoffice.domain.ShowId
import boxoffice.domain.ShowSeat
import boxoffice.domain.ViewerId
import boxoffice.store.BoxOfficeTables
import org.http4k.fanbelt.store.div
import org.http4k.fanbelt.store.eq
import java.time.Clock

class BoxOffice(private val tables: BoxOfficeTables, private val clock: Clock) {
    fun shows() = tables.shows.page().items

    fun show(id: ShowId) = tables.shows.get(id)

    fun bookings(show: ShowId) = tables.bookings.page((Booking::id / ShowSeat::show) eq show).items

    fun live(bookings: List<Booking>) = bookings.filter { it.liveAt(clock.instant()) }

    fun booking(pick: ShowSeat) = tables.bookings.get(pick)?.takeIf { it.liveAt(clock.instant()) }

    fun reserve(pick: ShowSeat, fan: ViewerId) {
        val booking = Booking(pick, fan, clock.instant() + RESERVED_FOR)
        if (tables.bookings.get(booking.id) == null) tables.bookings.insert(booking) else tables.bookings.update(booking.id) { booking }
    }

    fun release(booking: Booking) = tables.bookings.delete(booking.id)
}