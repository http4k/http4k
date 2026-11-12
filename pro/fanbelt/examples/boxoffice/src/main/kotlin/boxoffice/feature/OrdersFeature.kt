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
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.feature.Access.Denied
import org.http4k.fanbelt.feature.Outcome.Rejected
import java.time.Clock
import java.util.Random

class OrdersFeature(boxOffice: BoxOfficeFeature, clock: Clock, random: Random) : Feature<Orders, Actor>({ tables -> Orders(BoxOfficeTables(tables), clock, random) }) {
    val tickets by state(ViewerId) { orders, fan -> orders.of(fan) }

    val basket by derived { ctx, shopper: Shopper ->
        val reserved = ctx.domain.reserved(ctx.read(boxOffice.bookings, shopper.show), shopper.fan)
        reserved.forEach { ctx.staleAt(it.expiresAt) }
        Basket(reserved.map { it.id.seat }.sortedWith(seatOrder), ctx.read(tickets, shopper.fan).filter { it.show == shopper.show })
    }

    val fan by rule { _, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

    val system by rule { _, actor -> if (actor == Actor.System) Allowed(actor) else Denied }

    val checkout by operation(fan) { orders, _, id: ShowId, actor ->
        orders.checkout(id, actor.id) ?: return@operation Rejected(seatField, "Reserve some seats first")
        Changes.of(boxOffice.bookings.topic(id), tickets.topic(actor.id))
    }

    val paid by operation(system) { orders, _, id: OrderId, _ ->
        val order = orders.order(id) ?: return@operation Changes.none()
        orders.mark(id, OrderStatus.Confirmed)
        Changes.of(tickets.topic(order.fan))
    }

    val declined by operation(system) { orders, _, id: OrderId, _ ->
        val order = orders.order(id) ?: return@operation Changes.none()
        orders.mark(id, OrderStatus.Declined)
        orders.release(order)
        Changes.of(boxOffice.bookings.topic(order.show), tickets.topic(order.fan))
    }
}