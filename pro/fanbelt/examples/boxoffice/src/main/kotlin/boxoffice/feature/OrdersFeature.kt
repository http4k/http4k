package boxoffice.feature

import boxoffice.domain.Actor
import boxoffice.domain.Basket
import boxoffice.domain.OrderId
import boxoffice.domain.OrderStatus
import boxoffice.domain.Shopper
import boxoffice.domain.ShowId
import boxoffice.domain.ViewerId
import boxoffice.domain.seatField
import boxoffice.domain.seatOrder
import boxoffice.store.BoxOfficeTables
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Rejected
import java.time.Clock
import java.util.Random

class OrdersFeature(boxOffice: BoxOfficeFeature, clock: Clock, random: Random) : Feature<Orders, Actor>({ tables -> Orders(BoxOfficeTables(tables), clock, random) }) {
    val tickets by state { orders, fan: ViewerId -> orders.of(fan) }

    val basket by derived { ctx, shopper: Shopper ->
        val reserved = ctx.domain.reserved(ctx.read(boxOffice.bookings, shopper.show), shopper.fan)
        reserved.forEach { ctx.staleAt(it.expiresAt) }
        Basket(reserved.map { it.id.seat }.sortedWith(seatOrder), ctx.read(tickets, shopper.fan).filter { it.show == shopper.show })
    }

    val fan by rule { ctx, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

    val system by rule { ctx, actor -> if (actor == Actor.System) Allowed(actor) else Denied }

    val checkout by operation(fan) { orders, actor, id: ShowId, fan ->
        orders.checkout(id, fan.id) ?: return@operation Rejected(seatField, "Reserve some seats first")
        Changes.of(boxOffice.bookings.topic(id), tickets.topic(fan.id))
    }

    val paid by operation(system) { orders, actor, id: OrderId, system ->
        val order = orders.order(id) ?: return@operation Changes.none()
        orders.mark(id, OrderStatus.Confirmed)
        Changes.of(tickets.topic(order.fan))
    }

    val declined by operation(system) { orders, actor, id: OrderId, system ->
        val order = orders.order(id) ?: return@operation Changes.none()
        orders.mark(id, OrderStatus.Declined)
        orders.release(order)
        Changes.of(boxOffice.bookings.topic(order.show), tickets.topic(order.fan))
    }
}
