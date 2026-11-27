package boxoffice.feature

import boxoffice.domain.Actor
import boxoffice.domain.Basket
import boxoffice.domain.Order
import boxoffice.domain.OrderId
import boxoffice.domain.OrderStatus
import boxoffice.domain.Shopper
import boxoffice.domain.ShowId
import boxoffice.domain.ViewerId
import boxoffice.domain.seatField
import boxoffice.domain.seatOrder
import boxoffice.store.BoxOfficeTables
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.body.form
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.tenancy.Scope

class OrdersFeature(boxOffice: BoxOfficeFeature, private val runtime: AppRuntime) : Feature<Orders, Actor>({ tables -> Orders(BoxOfficeTables(tables), runtime.clock, runtime.random) }) {
    val tickets by state(ViewerId) { orders, fan -> orders.of(fan) }

    val basket by derived { ctx, shopper: Shopper ->
        val reserved = ctx.domain.reserved(boxOffice.bookings.read(ctx, shopper.show), shopper.fan)
        reserved.forEach { ctx.stale(at = it.expiresAt) }
        Basket(reserved.map { it.id.seat }.sortedWith(seatOrder), tickets.read(ctx, shopper.fan).filter { it.show == shopper.show })
    }

    val fan by rule { _, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

    val system by rule { _, actor -> if (actor == Actor.System) Allowed(actor) else Denied }

    val checkout by operation(fan) { orders, _, id: ShowId, fan ->
        val order = orders.checkout(id, fan.id) ?: return@operation Rejected(seatField, "Reserve some seats first")
        Changes.of(boxOffice.bookings.topic(id), tickets.topic(fan.id), charge.topic(order.id))
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

    val order by state(OrderId) { orders, id -> orders.order(id) }

    val charge by job(OrderId) { scope, id ->
        order.read(scope, id)?.takeIf { it.status == OrderStatus.Pending }?.let { take(scope, it) }
    }

    private fun take(scope: Scope, order: Order) {
        val charged = runtime.http(Request(POST, "/charge").form("order", order.id.toString()).form("seats", order.seats.size.toString()))
        when {
            charged.status.successful -> paid.execute(scope, Actor.System, order.id)
            charged.status == PAYMENT_REQUIRED -> declined.execute(scope, Actor.System, order.id)
            else -> error("Charge for ${order.id} failed: ${charged.status}")
        }
    }
}
