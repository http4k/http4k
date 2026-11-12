package boxoffice.feature

import boxoffice.domain.Booking
import boxoffice.domain.Order
import boxoffice.domain.OrderId
import boxoffice.domain.OrderStatus
import boxoffice.domain.ShowId
import boxoffice.domain.ShowSeat
import boxoffice.domain.ViewerId
import boxoffice.domain.seatOrder
import boxoffice.store.BoxOfficeTables
import dev.forkhandles.values.random
import org.http4k.fanbelt.store.Order.Newest
import org.http4k.fanbelt.store.div
import org.http4k.fanbelt.store.eq
import java.time.Clock
import java.util.Random

class Orders(private val tables: BoxOfficeTables, private val clock: Clock, private val random: Random) {
    fun of(fan: ViewerId) = tables.orders.page(Order::fan eq fan, Newest).items

    fun order(id: OrderId) = tables.orders.get(id)

    fun pending() = tables.orders.page(Order::status eq OrderStatus.Pending).items

    fun reserved(bookings: List<Booking>, fan: ViewerId) = bookings.filter { it.fan == fan && it.order == null && it.liveAt(clock.instant()) }

    fun checkout(show: ShowId, fan: ViewerId): Order? {
        val reserved = reserved(tables.bookings.page((Booking::id / ShowSeat::show) eq show).items, fan)
        if (reserved.isEmpty()) return null
        val order = Order(OrderId.random(random), show, fan, reserved.map { it.id.seat }.sortedWith(seatOrder), OrderStatus.Pending, clock.instant())
        tables.orders.insert(order)
        reserved.forEach { booking -> tables.bookings.update(booking.id) { it.copy(order = order.id) } }
        return order
    }

    fun mark(id: OrderId, status: OrderStatus) = tables.orders.update(id) { it.copy(status = status) }

    fun release(order: Order) = tables.bookings.page(Booking::order eq order.id).items.forEach { tables.bookings.delete(it.id) }
}