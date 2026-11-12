package boxoffice.domain

import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import org.http4k.fanbelt.store.Identified
import java.time.Instant
import java.util.UUID

class OrderId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<OrderId>(::OrderId)
}

enum class OrderStatus { Pending, Confirmed, Declined }

data class Order(
    override val id: OrderId,
    val show: ShowId,
    val fan: ViewerId,
    val seats: List<SeatRef>,
    val status: OrderStatus,
    val at: Instant,
) : Identified<OrderId>

data class Shopper(val show: ShowId, val fan: ViewerId)

data class Basket(val reserved: List<SeatRef>, val tickets: List<Order>)